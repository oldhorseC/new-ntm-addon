#requires -Version 5.1
<#
    update-doc-index.ps1 - keeps the "LLM reading index" at the top of the NTM-IT docs fresh.

    The generated block (between the LLM-INDEX markers) contains:
      1. a short reading guide for AI readers,
      2. the section index  : section -> line range -> grep keywords,
      3. the keyword locator: keyword -> section / line range,
      4. the task router    : "I want to do X" -> which sections to read.

    Line ranges are recomputed until they are stable, so they always match the file
    as it ends up on disk.  Files are kept as UTF-8 without BOM with CRLF endings.

    Usage:
        .\update-doc-index.ps1                    regenerate both documents
        .\update-doc-index.ps1 -Check             validate only, write nothing
        .\update-doc-index.ps1 -Name README.md -Check

    Exit code: 0 = ok, 1 = at least one check failed.
#>
[CmdletBinding()]
param(
    [string]$Root = 'D:\new mod',
    [string[]]$Name,
    [switch]$Check
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:BeginMark = '<!-- LLM-INDEX:BEGIN'
$script:EndMark   = '<!-- LLM-INDEX:END -->'

$script:StopWords = @(
    'the','and','for','not','with','this','that','from','are','was','were','has','have','will','can','must',
    'true','false','null','default','required','after','before','only','when','then','else','file','line',
    'java','json','string','list','void','public','private','static','class','return','import','package',
    'http','https','www','src','main','test','gradle','buildscript','buildscripts'
)

# --------------------------------------------------------------------------- I/O

function Get-DocLines {
    param([string]$Path)
    $text = [System.IO.File]::ReadAllText($Path, $script:Utf8NoBom)
    $text = $text.Replace("`r`n", "`n").Replace("`r", "`n")
    return [string[]]($text -split "`n")
}

function Save-DocLines {
    param([string]$Path, [string[]]$Lines)
    $text = [string]::Join("`r`n", $Lines)
    $tmp = "$Path.tmp"
    [System.IO.File]::WriteAllText($tmp, $text, $script:Utf8NoBom)
    Move-Item -LiteralPath $tmp -Destination $Path -Force
}

function Test-NoBom {
    param([string]$Path)
    $b = [System.IO.File]::ReadAllBytes($Path)
    if ($b.Length -lt 3) { return $true }
    return -not ($b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF)
}

function Test-CrlfOnly {
    param([string]$Path)
    $t = [System.IO.File]::ReadAllText($Path, $script:Utf8NoBom)
    $lf = ([regex]::Matches($t, "`n")).Count
    $crlf = ([regex]::Matches($t, "`r`n")).Count
    return ($lf -eq $crlf)
}

# ---------------------------------------------------------------------- parsing

function Get-Sections {
    <#
        Returns @{ Sections = @(...); FencedHeadings = @(line numbers) }.
        Headings inside ``` / ~~~ fences are ignored (and reported separately),
        because shell comments such as "# comment" are not document headings.
    #>
    param([string[]]$Lines)
    $inFence = $false
    $sections = New-Object System.Collections.ArrayList
    $fenced = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        if ($line -match '^\s*(```|~~~)') { $inFence = -not $inFence; continue }
        if ($inFence) {
            if ($line -match '^#{1,6}\s') { [void]$fenced.Add($i + 1) }
            continue
        }
        $m = [regex]::Match($line, '^(#{1,6})\s+(\S.*?)\s*$')
        if ($m.Success) {
            [void]$sections.Add([pscustomobject]@{
                Level   = $m.Groups[1].Value.Length
                Text    = $m.Groups[2].Value
                Start   = $i + 1
                End     = 0
                LeafEnd = 0
                Id      = ''
                Ordinal = 0
            })
        }
    }
    for ($i = 0; $i -lt $sections.Count; $i++) {
        $end = $Lines.Count
        for ($j = $i + 1; $j -lt $sections.Count; $j++) {
            if ($sections[$j].Level -le $sections[$i].Level) { $end = $sections[$j].Start - 1; break }
        }
        while ($end -gt $sections[$i].Start -and $Lines[$end - 1].Trim() -eq '') { $end-- }
        $sections[$i].End = $end

        $leaf = $Lines.Count
        if ($i + 1 -lt $sections.Count) { $leaf = $sections[$i + 1].Start - 1 }
        while ($leaf -gt $sections[$i].Start -and $Lines[$leaf - 1].Trim() -eq '') { $leaf-- }
        $sections[$i].LeafEnd = $leaf
    }
    return [pscustomobject]@{ Sections = $sections; FencedHeadings = $fenced }
}

function Set-SectionIds {
    param($Sections, [bool]$Numbered)
    $used = New-Object System.Collections.Generic.HashSet[string]
    $ord = 0
    foreach ($s in $Sections) {
        if ($s.Level -le 1) { continue }
        $ord++
        $s.Ordinal = $ord
        $id = $null
        if ($Numbered) {
            $m = [regex]::Match($s.Text, '^([A-Za-z]{0,2}\d+(?:\.\d+)*|[A-Z])\.?(?:\s|$)')
            if ($m.Success) { $id = $m.Groups[1].Value }
        }
        if (-not $id) { $id = "R$ord" }
        $base = $id; $n = 1
        while ($used.Contains($id)) { $n++; $id = "$base-$n" }
        [void]$used.Add($id)
        $s.Id = $id
    }
}
# ------------------------------------------------------------------- keywords

function Add-Token {
    # Counters live in two plain hashtables (int counts + display spelling).  Never store a
    # PSCustomObject here: a note property named "Count" collides with PSObject's own .Count
    # member and PowerShell then mangles the dictionary entries.
    param([hashtable]$Counts, [hashtable]$Display, [string]$Token)
    if ([string]::IsNullOrWhiteSpace($Token)) { return }
    $t = $Token.Trim()
    $t = $t -replace '\.java$', '' -replace '\(\)$', '' -replace '\.$', ''
    if ($t.Length -lt 4) { return }
    if ($t -notmatch '^[A-Za-z_][A-Za-z0-9_.:\-]*$') { return }
    $key = $t.ToLowerInvariant()
    if ($script:StopWords -contains $key) { return }
    if ($Counts.ContainsKey($key)) {
        $Counts[$key] = ([int]$Counts[$key]) + 1
        if (($t -cmatch '[A-Z]') -and ([string]$Display[$key] -cnotmatch '[A-Z]')) { $Display[$key] = $t }
    } else {
        $Counts[$key] = 1
        $Display[$key] = $t
    }
}

function Get-TokenScore {
    param([string]$Token, [int]$Count)
    $bonus = 0
    if ($Token -cmatch '[a-z][A-Z]') { $bonus++ }
    if ($Token -cmatch '^[A-Z][a-z]') { $bonus++ }
    if ($Token -match '_') { $bonus++ }
    if ($Token -match ':') { $bonus++ }
    return $Count * (1 + $bonus)
}

function Get-RangeTokens {
    param([string[]]$Lines, [int]$Start, [int]$End, [int]$Top = 4, [int]$MinCount = 2)
    $counts = @{}
    $disp = @{}
    for ($i = $Start - 1; $i -lt $End; $i++) {
        $line = $Lines[$i]
        foreach ($m in [regex]::Matches($line, '`([^`]+)`')) { Add-Token -Counts $counts -Display $disp -Token $m.Groups[1].Value }
        foreach ($m in [regex]::Matches($line, '\b([A-Za-z_][A-Za-z0-9_]{3,})\b')) { Add-Token -Counts $counts -Display $disp -Token $m.Groups[1].Value }
    }
    $scored = New-Object System.Collections.ArrayList
    foreach ($k in @($counts.Keys)) {
        $tok = [string]$disp[$k]
        if ([string]::IsNullOrWhiteSpace($tok)) { $tok = [string]$k }
        $n = [int]$counts[$k]
        [void]$scored.Add([pscustomobject]@{
                Token = $tok
                Key   = [string]$k
                N     = $n
                Score = (Get-TokenScore -Token $tok -Count $n)
            })
    }
    $picked = @($scored | Where-Object { $_.N -ge $MinCount } | Sort-Object -Property Score, N -Descending | Select-Object -First $Top)
    if ($picked.Count -eq 0 -and $scored.Count -gt 0) {
        $picked = @($scored | Sort-Object -Property Score, N -Descending | Select-Object -First ([Math]::Min(2, $Top)))
    }
    return $picked
}

function Get-GlobalKeywords {
    param([string[]]$Lines, $Sections, [int]$MinCount = 3, [int]$Top = 40)
    $leaf = @($Sections | Where-Object { $_.Level -gt 1 })
    if ($leaf.Count -eq 0) { return @() }
    $total = @{}
    $disp = @{}
    $sect = @{}
    foreach ($s in $leaf) {
        foreach ($k in (Get-RangeTokens -Lines $Lines -Start $s.Start -End $s.LeafEnd -Top 400 -MinCount 1)) {
            $key = [string]$k.Key
            if (-not $total.ContainsKey($key)) {
                $total[$key] = 0
                $disp[$key] = [string]$k.Token
                $sect[$key] = New-Object System.Collections.ArrayList
            }
            $total[$key] = ([int]$total[$key]) + [int]$k.N
            [void]$sect[$key].Add(($s.Id + '~' + $s.Start + '~' + $s.End + '~' + [int]$k.N))
        }
    }
    $rows = New-Object System.Collections.ArrayList
    foreach ($key in @($total.Keys)) {
        $sum = [int]$total[$key]
        if ($sum -lt $MinCount) { continue }
        $raw = @($sect[$key])
        if (($raw.Count / [double]$leaf.Count) -gt 0.35) { continue }
        $ranked = @($raw | Sort-Object -Property @{ Expression = { [int]($_ -split '~')[3] }; Descending = $true } | Select-Object -First 3)
        $entries = New-Object System.Collections.ArrayList
        foreach ($r in $ranked) {
            $p = $r -split '~'
            [void]$entries.Add([pscustomobject]@{ Id = $p[0]; Start = [int]$p[1]; End = [int]$p[2] })
        }
        [void]$rows.Add([pscustomobject]@{ Token = [string]$disp[$key]; Total = $sum; Entries = $entries })
    }
    return @($rows | Sort-Object -Property Total -Descending | Select-Object -First $Top)
}
# ----------------------------------------------------------------- block text

function Get-BlockLabels {
    param([string]$Lang)
    if ($Lang -eq 'zh') {
        return @{
            Guide        = '**给 AI 读者的读取指引（人类可跳过）**'
            IsWhat       = '本文件是什么'
            Steps        = '读取三步'
            StepsText    = '① 读下面「索引」定位小节 → ② 按「行号区间」只读该区间（父节区间含其子节）→ ③ 只有需要全量检索时才搜索'
            Recipes      = '检索配方'
            RecipesNote  = '（PowerShell 5.1 已验证；务必带 -Encoding UTF8，否则中文乱码）'
            Pitch        = '标题约定'
            PitchText    = '`## N. 标题` / `### N.M 标题`；本区块与代码块内的 `#` 行都不是标题'
            NoVerify     = '免核查'
            NoVerifyText = '默认信任本文档，不要为了核对本文档去通读源码（见 `RULE.md` 的两条信道）；确需核对请先说一句'
            Sibling      = '姊妹文档'
            IndexTitle   = '**索引（小节 → 行号 → 关键词）**'
            IndexHead    = '| § | 小节 | 行号 | 关键词（可直接 grep） |'
            LocTitle     = '**关键词定位表（先查这里，再按行号精读）**'
            LocHead      = '| 关键词 | 出现于（§ 行号） |'
            RouterTitle  = '**任务路由（我要做 X → 读这些节）**'
            RouterHead   = '| 任务 | 读这些节 |'
            Footer       = '*本区块由 `tools/update-doc-index.ps1` 生成，请勿手改；文档改完后重跑该脚本即可刷新。*'
            ListJob      = '列全部小节'
            FindJob      = '关键词 → 行号'
        }
    }
    return @{
        Guide        = '**Reading guide for AI agents (humans can skip this)**'
        IsWhat       = 'What this file is'
        Steps        = 'How to read it'
        StepsText    = '1) locate the section in the Index below, 2) read only that line range, 3) search the whole file only when you must'
        Recipes      = 'Search recipes'
        RecipesNote  = '(verified on PowerShell 5.1)'
        Pitch        = 'Heading conventions'
        PitchText    = '`## N. Title` / `### N.M Title`; a `#` line inside this block or inside a code fence is not a heading'
        NoVerify     = 'Do not re-verify'
        NoVerifyText = 'Trust this document by default; do not re-read the source tree to audit it (see the two-channel rule in `RULE.md`); ask first if a check is really needed'
        Sibling      = 'Sibling document'
        IndexTitle   = '**Index (section -> line range -> keywords)**'
        IndexHead    = '| Id | Section | Lines | Keywords (grep-ready) |'
        LocTitle     = '**Keyword locator (look here first, then read by line range)**'
        LocHead      = '| Keyword | Occurs in (id, lines) |'
        RouterTitle  = '**Task router (I want to do X -> read these sections)**'
        RouterHead   = '| Task | Sections to read |'
        Footer       = '*Generated by `tools/update-doc-index.ps1` - do not hand-edit; re-run the script after editing the document.*'
        ListJob      = 'list all sections'
        FindJob      = 'keyword -> line number'
    }
}
function Get-MappedLine {
    param([int]$N, [int]$Shift, [int]$ShiftFrom)
    if ($N -ge $ShiftFrom) { return $N + $Shift }
    return $N
}

function Resolve-SectionRef {
    param($Sections, [string]$Ref)
    $hit = @($Sections | Where-Object { $_.Level -gt 1 -and $_.Id -eq $Ref })
    if ($hit.Count -eq 0) {
        $hit = @($Sections | Where-Object { $_.Level -gt 1 -and $_.Text.IndexOf($Ref, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 })
    }
    return $hit
}

function New-IndexBlock {
    param($Doc, [string[]]$Body, $Sections, $GlobalRows, [int]$Shift, [int]$ShiftFrom)
    $L = Get-BlockLabels -Lang $Doc.Lang
    $out = New-Object System.Collections.ArrayList
    $nl = "`n"

    [void]$out.Add($script:BeginMark + ' -->')
    [void]$out.Add($L.Guide)
    [void]$out.Add('')
    [void]$out.Add('- **' + $L.IsWhat + '**: ' + $Doc.Summary)
    [void]$out.Add('- **' + $L.Steps + '**: ' + $L.StepsText)
    [void]$out.Add('- **' + $L.Recipes + '** ' + $L.RecipesNote + ':')
    [void]$out.Add('    - ' + $L.ListJob + ': ``Select-String -Path ' + [char]39 + $Doc.File + [char]39 + ' -Pattern ' + [char]39 + '^#{2,3} ' + [char]39 + ' -Encoding UTF8``')
    [void]$out.Add('    - ' + $L.FindJob + ': ``Select-String -Path ' + [char]39 + $Doc.File + [char]39 + ' -Pattern ' + [char]39 + '<keyword>' + [char]39 + ' -Encoding UTF8``')
    [void]$out.Add('- **' + $L.Pitch + '**: ' + $L.PitchText)
    [void]$out.Add('- **' + $L.NoVerify + '**: ' + $L.NoVerifyText)
    [void]$out.Add('- **' + $L.Sibling + '**: ' + $Doc.Sibling)
    [void]$out.Add('')

    [void]$out.Add($L.IndexTitle)
    [void]$out.Add('')
    [void]$out.Add($L.IndexHead)
    [void]$out.Add('|---|---|---|---|')
    foreach ($s in $Sections) {
        if ($s.Level -le 1) { continue }
        $indent = ''
        for ($k = 2; $k -lt $s.Level; $k++) { $indent += ([char]0x21B3).ToString() + ' ' }
        $kw = @()
        $seg = ($Body[($s.Start - 1)..($s.LeafEnd - 1)] -join "`n")
        foreach ($t in (Get-RangeTokens -Lines $Body -Start $s.Start -End $s.LeafEnd -Top 4 -MinCount 2)) {
            $tk = [string]$t.Token
            if ([string]::IsNullOrWhiteSpace($tk) -or $tk.Length -lt 3) { continue }
            # keep only terms that really occur in this section, so a row can never point
            # at a keyword its own line range does not contain
            if ($seg.IndexOf($tk, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { $kw += $tk }
        }
        if (($kw -join ', ').Trim() -eq '') {
            # No identifier was repeated in this section: fall back to the heading text itself,
            # which is always inside the row's line range, so the keyword stays greppable.
            $fb = ([regex]::Split($s.Text, '[（(—\-]'))[0].Trim()
            if ($fb.Length -gt 20) {
                $cut = $fb.Substring(0, 20)
                $sp = $cut.LastIndexOf(' ')
                if ($sp -gt 6) { $cut = $cut.Substring(0, $sp) }
                $fb = $cut
            }
            if ($fb -ne '') { $kw += $fb }
        }
        $start = Get-MappedLine -N $s.Start -Shift $Shift -ShiftFrom $ShiftFrom
        $end = Get-MappedLine -N $s.End -Shift $Shift -ShiftFrom $ShiftFrom
        [void]$out.Add('| ' + $s.Id + ' | ' + $indent + $s.Text + ' | ' + $start + '-' + $end + ' | ' + ($kw -join ', ') + ' |')
    }
    [void]$out.Add('')

    [void]$out.Add($L.LocTitle)
    [void]$out.Add('')
    [void]$out.Add($L.LocHead)
    [void]$out.Add('|---|---|')
    foreach ($g in $GlobalRows) {
        $parts = @()
        foreach ($e in $g.Entries) {
            $s1 = Get-MappedLine -N $e.Start -Shift $Shift -ShiftFrom $ShiftFrom
            $e1 = Get-MappedLine -N $e.End -Shift $Shift -ShiftFrom $ShiftFrom
            $parts += ($e.Id + ' (' + $s1 + '-' + $e1 + ')')
        }
        [void]$out.Add('| `' + $g.Token + '` | ' + ($parts -join '; ') + ' |')
    }
    [void]$out.Add('')

    [void]$out.Add($L.RouterTitle)
    [void]$out.Add('')
    [void]$out.Add($L.RouterHead)
    [void]$out.Add('|---|---|')
    foreach ($r in $Doc.Router) {
        $parts = @()
        foreach ($ref in $r.Refs) {
            foreach ($s in (Resolve-SectionRef -Sections $Sections -Ref $ref)) {
                if ($s.Level -le 1) { continue }
                $start = Get-MappedLine -N $s.Start -Shift $Shift -ShiftFrom $ShiftFrom
                $end = Get-MappedLine -N $s.End -Shift $Shift -ShiftFrom $ShiftFrom
                $parts += ('§' + $s.Id + ' ' + $s.Text + ' (' + $start + '-' + $end + ')')
            }
        }
        [void]$out.Add('| ' + $r.Label + ' | ' + ($parts -join '; ') + ' |')
    }
    [void]$out.Add('')
    [void]$out.Add($L.Footer)
    [void]$out.Add($script:EndMark)
    return [string[]]$out
}
# ------------------------------------------------------------ block placement

function Remove-IndexBlock {
    param([string[]]$Lines)
    $begin = -1; $end = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($begin -lt 0 -and $Lines[$i].StartsWith($script:BeginMark)) { $begin = $i; continue }
        if ($begin -ge 0 -and $Lines[$i].Trim() -eq $script:EndMark) { $end = $i; break }
    }
    if ($begin -lt 0 -or $end -lt 0) { return [string[]]$Lines }
    $from = $begin; $to = $end
    if ($from -gt 0 -and $Lines[$from - 1].Trim() -eq '') { $from-- }
    if (($to + 1) -lt $Lines.Count -and $Lines[$to + 1].Trim() -eq '') { $to++ }
    $res = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $from; $i++) { [void]$res.Add($Lines[$i]) }
    for ($i = $to + 1; $i -lt $Lines.Count; $i++) { [void]$res.Add($Lines[$i]) }
    return [string[]]$res
}

function Insert-IndexBlock {
    param($Doc, [string[]]$Body, [string[]]$Block)
    $idx = -1
    for ($i = 0; $i -lt $Body.Count; $i++) {
        if ($Body[$i] -match $Doc.Anchor) { $idx = $i; break }
    }
    if ($idx -lt 0) { throw ('anchor not found in ' + $Doc.File + ' : ' + $Doc.Anchor) }
    $limit = $idx
    $shiftFrom = $idx + 1
    if ($Doc.AnchorMode -ne 'Before') { $limit = $idx + 1; $shiftFrom = $idx + 2 }
    $res = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $limit; $i++) { [void]$res.Add($Body[$i]) }
    [void]$res.Add('')
    foreach ($b in $Block) { [void]$res.Add($b) }
    [void]$res.Add('')
    for ($i = $limit; $i -lt $Body.Count; $i++) { [void]$res.Add($Body[$i]) }
    return [pscustomobject]@{ Lines = [string[]]$res; ShiftFrom = $shiftFrom }
}

function Get-DocResult {
    param($Doc, [string[]]$FileLines)
    $body = Remove-IndexBlock -Lines $FileLines
    $parsed = Get-Sections -Lines $body
    $secs = $parsed.Sections
    Set-SectionIds -Sections $secs -Numbered $Doc.Numbered
    $global = Get-GlobalKeywords -Lines $body -Sections $secs -MinCount $Doc.MinTokenCount -Top $Doc.MaxLocatorRows
    $block = @()
    $shift = 0
    $shiftFrom = 0
    for ($iter = 0; $iter -lt 5; $iter++) {
        $placed = Insert-IndexBlock -Doc $Doc -Body $body -Block $block
        $shiftFrom = $placed.ShiftFrom
        $candidate = New-IndexBlock -Doc $Doc -Body $body -Sections $secs -GlobalRows $global -Shift $shift -ShiftFrom $shiftFrom
        if ($iter -gt 0 -and (($candidate -join "`n") -eq ($block -join "`n"))) { break }
        $block = $candidate
        $shift = $block.Count + 2
    }
    $final = Insert-IndexBlock -Doc $Doc -Body $body -Block $block
    return [pscustomobject]@{
        Lines    = $final.Lines
        Body     = $body
        Sections = $secs
        Fenced   = $parsed.FencedHeadings
        Shift    = $block.Count + 2
        Rows     = $secs.Count
    }
}

function Get-FirstDiff {
    # NOTE: PowerShell variable names are case-insensitive, so the loop variables below
    # must not be called $a/$b - that would overwrite the [string[]]$A/$B parameters.
    param([string[]]$A, [string[]]$B)
    $left = @($A)
    $right = @($B)
    $n = [Math]::Max($left.Count, $right.Count)
    for ($i = 0; $i -lt $n; $i++) {
        $x = '<eof>'
        $y = '<eof>'
        if ($i -lt $left.Count) { $x = [string]$left[$i] }
        if ($i -lt $right.Count) { $y = [string]$right[$i] }
        if ($x -ne $y) { return ('line ' + ($i + 1) + ': disk [' + $x + '] vs expected [' + $y + ']') }
    }
    return '<none>'
}
# ----------------------------------------------------------------- validation

function Test-Document {
    param($Doc, [string[]]$Lines, $Result, [string]$Path)
    $checks = New-Object System.Collections.ArrayList
    $text = ($Lines -join "`n")

    $beginCount = 0; $endCount = 0; $beginAt = -1; $endAt = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i].StartsWith($script:BeginMark)) { $beginCount++; $beginAt = $i }
        if ($Lines[$i].Trim() -eq $script:EndMark) { $endCount++; $endAt = $i }
    }
    [void]$checks.Add([pscustomobject]@{ Ok = ($beginCount -eq 1 -and $endCount -eq 1 -and $beginAt -lt $endAt); Name = 'markers'; Detail = ("BEGIN=$beginCount END=$endCount") })

    $expected = ($Result.Lines -join "`n")
    [void]$checks.Add([pscustomobject]@{ Ok = ($expected -eq $text); Name = 'index up to date'; Detail = (Get-FirstDiff -A $Lines -B $Result.Lines) })

    [void]$checks.Add([pscustomobject]@{ Ok = ($Result.Fenced.Count -eq 0); Name = 'no heading-like line inside code fences'; Detail = ('fenced=' + ($Result.Fenced -join ',')) })

    $h1 = @($Result.Sections | Where-Object { $_.Level -le 1 })
    [void]$checks.Add([pscustomobject]@{ Ok = ($h1.Count -eq 1); Name = 'exactly one H1'; Detail = ('h1=' + $h1.Count) })

    $rowRegex = '(?m)^\|\s*([^|]+?)\s*\|\s*((?:↳\s*)*)(.+?)\s*\|\s*(\d+)-(\d+)\s*\|\s*(.*?)\s*\|$'
    $rows = @([regex]::Matches($text, $rowRegex))
    $secCount = @($Result.Sections | Where-Object { $_.Level -gt 1 }).Count
    [void]$checks.Add([pscustomobject]@{ Ok = ($rows.Count -eq $secCount); Name = 'index rows == sections'; Detail = ('rows=' + $rows.Count + ' sections=' + $secCount) })

    $badAnchor = @()
    $badKeyword = @()
    foreach ($m in $rows) {
        $id = $m.Groups[1].Value
        $title = $m.Groups[3].Value
        $s = [int]$m.Groups[4].Value
        $e = [int]$m.Groups[5].Value
        if ($s -lt 1 -or $e -gt $Lines.Count -or $e -lt $s) { $badAnchor += ($id + ' range ' + $s + '-' + $e); continue }
        $hm = [regex]::Match($Lines[$s - 1], '^#{1,6}\s+(.+?)\s*$')
        if (-not $hm.Success -or $hm.Groups[1].Value -ne $title) { $badAnchor += ($id + ' expected heading [' + $title + '] at line ' + $s) }
        $seg = ($Lines[($s - 1)..($e - 1)] -join "`n")
        foreach ($kw in ($m.Groups[6].Value -split ', ')) {
            $k = $kw.Trim()
            if ($k -eq '') { continue }
            if ($seg.IndexOf($k, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) { $badKeyword += ($id + ' keyword [' + $k + '] not in ' + $s + '-' + $e) }
        }
    }
    [void]$checks.Add([pscustomobject]@{ Ok = ($badAnchor.Count -eq 0); Name = 'row anchors point at their heading'; Detail = ($badAnchor -join '; ') })
    [void]$checks.Add([pscustomobject]@{ Ok = ($badKeyword.Count -eq 0); Name = 'row keywords occur in their range'; Detail = ($badKeyword | Select-Object -First 5) -join '; ' })
    $locRegex = '(?m)^\|\s*`([^`]+)`\s*\|\s*(.+?)\s*\|\s*$'
    $locRows = @([regex]::Matches($text, $locRegex))
    $badLoc = @()
    foreach ($m in $locRows) {
        $tok = $m.Groups[1].Value
        $cell = $m.Groups[2].Value
        foreach ($e in [regex]::Matches($cell, '([^;()]+?)\s*\((\d+)-(\d+)\)')) {
            $s = [int]$e.Groups[2].Value
            $en = [int]$e.Groups[3].Value
            if ($s -lt 1 -or $en -gt $Lines.Count -or $en -lt $s) { $badLoc += ($tok + ' range ' + $s + '-' + $en); continue }
            $seg = ($Lines[($s - 1)..($en - 1)] -join "`n")
            if ($seg.IndexOf($tok, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) { $badLoc += ($tok + ' not in ' + $s + '-' + $en) }
        }
    }
    [void]$checks.Add([pscustomobject]@{ Ok = ($badLoc.Count -eq 0); Name = 'locator keywords occur in their range'; Detail = (($badLoc | Select-Object -First 5) -join '; ') })

    $blockText = ''
    if ($beginAt -ge 0 -and $endAt -gt $beginAt) { $blockText = ($Lines[$beginAt..$endAt] -join "`n") }
    $emptyCells = @([regex]::Matches($blockText, '(?m)^\|.*\|\s*\|\s*$'))
    [void]$checks.Add([pscustomobject]@{ Ok = ($emptyCells.Count -eq 0); Name = 'no empty table cell in the index block'; Detail = ('empty=' + $emptyCells.Count) })

    $noBom = Test-NoBom -Path $Path
    $crlf = Test-CrlfOnly -Path $Path
    $raw = [System.IO.File]::ReadAllBytes($Path)
    [void]$checks.Add([pscustomobject]@{ Ok = ($noBom -and $crlf); Name = 'UTF-8 without BOM + CRLF only'; Detail = ('noBom=' + $noBom + ' crlfOnly=' + $crlf + ' bytes=' + $raw.Length) })

    return $checks
}
# --------------------------------------------------------------------- config

$Docs = @(
    [pscustomobject]@{
        File           = 'SERVER_PLAN.md'
        Lang           = 'zh'
        Numbered       = $true
        Anchor         = '^---\s*$'
        AnchorMode     = 'After'
        MinTokenCount  = 3
        MaxLocatorRows = 40
        Summary        = '服务器多方块的定稿设计纲要：架构 / 数据层 / 接口 / GUI / 阶段计划 / 风险与 ADR；文中 `文件:行号` 是 2026-09-26 的规划期快照。'
        Sibling        = '`README.md`（构建环境 / 已注册内容 / 踩坑，English）'
        Router         = @(
            [pscustomobject]@{ Label = '要动手写代码'; Refs = @('9', '4', '5.5', '6', '7') },
            [pscustomobject]@{ Label = '要评估风险 / 看决策记录'; Refs = @('2', '11') },
            [pscustomobject]@{ Label = '要查 HBM 某行为在某处'; Refs = @('12', 'D') },
            [pscustomobject]@{ Label = '要跑编译 / 起客户端 / 查日志'; Refs = @('10.1', '10.2') },
            [pscustomobject]@{ Label = '要知道新增了哪些类 / NBT 键 / 事件名'; Refs = @('A', 'B', 'C') },
            [pscustomobject]@{ Label = '要确认 Mixin 与环境前提'; Refs = @('P0.0', 'E', '2.4') }
        )
    },
    [pscustomobject]@{
        File           = 'README.md'
        Lang           = 'en'
        Numbered       = $false
        Anchor         = '^## Metadata'
        AnchorMode     = 'Before'
        MinTokenCount  = 2
        MaxLocatorRows = 30
        Summary        = 'NTM-IT addon project: build/run environment, the two registered blocks and the traps that cost time.'
        Sibling        = '`SERVER_PLAN.md` (server multiblock design plan, Chinese)'
        Router         = @(
            [pscustomobject]@{ Label = 'Add the next block end to end'; Refs = @('Copy this to add the next block') },
            [pscustomobject]@{ Label = 'Build / run / verify the addon'; Refs = @('Commands', 'Verification performed') },
            [pscustomobject]@{ Label = 'Fix environment problems (JDK, Gradle daemon, MixinBooter)'; Refs = @('Environment', 'Why the Gradle daemon runs on JDK 17', 'Mixins and the MixinBooter') },
            [pscustomobject]@{ Label = 'Know what is already registered in game'; Refs = @('Current state', 'Registered content') },
            [pscustomobject]@{ Label = 'Avoid the traps that cost the most time'; Refs = @('Lessons that cost the most time', 'Model JSON gotchas', 'Blockstate variants', '1.12.2 API traps met here') }
        )
    }
)

# ----------------------------------------------------------------------- main

$targets = @($Docs | Where-Object { (-not $Name) -or ($Name -contains $_.File) })
if ($Name) {
    foreach ($n in $Name) {
        if (@($Docs | Where-Object { $_.File -eq $n }).Count -eq 0) { throw ('unknown document: ' + $n) }
    }
}

$failed = 0
foreach ($d in $targets) {
    $path = Join-Path $Root $d.File
    if (-not (Test-Path -LiteralPath $path)) { throw ('missing file: ' + $path) }
    $lines = Get-DocLines -Path $path
    $res = Get-DocResult -Doc $d -FileLines $lines

    Write-Host ''
    Write-Host ('== ' + $d.File + ' ==') -ForegroundColor Cyan
    Write-Host ('   lines: ' + $lines.Count + ' -> ' + $res.Lines.Count + '   (index block: ' + $res.Shift + ' lines)')
    Write-Host ('   sections indexed: ' + @($res.Sections | Where-Object { $_.Level -gt 1 }).Count)

    if ($Check) {
        $checks = Test-Document -Doc $d -Lines $lines -Result $res -Path $path
    } else {
        $bak = Join-Path $Root ('tools\backup\' + $d.File + '.' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.bak')
        Copy-Item -LiteralPath $path -Destination $bak -Force
        Save-DocLines -Path $path -Lines $res.Lines
        $after = Get-DocLines -Path $path
        $checks = Test-Document -Doc $d -Lines $after -Result (Get-DocResult -Doc $d -FileLines $after) -Path $path
        Write-Host ('   backup: ' + $bak)
    }

    foreach ($c in $checks) {
        $tag = '[FAIL]'
        $color = 'Red'
        if ($c.Ok) { $tag = '[ OK ]'; $color = 'Green' }
        $detail = ''
        if ($c.Detail) { $detail = '  -- ' + [string]$c.Detail }
        Write-Host ('   ' + $tag + ' ' + $c.Name + $detail) -ForegroundColor $color
    }
    if (@($checks | Where-Object { -not $_.Ok }).Count -gt 0) { $failed++ }

    if (-not $Check) {
        Write-Host '   verify with: .\update-doc-index.ps1 -Check' -ForegroundColor DarkGray
    }
}

Write-Host ''
if ($failed -gt 0) {
    Write-Host ('FAILED: ' + $failed + ' document(s) did not pass the checks.') -ForegroundColor Red
    exit 1
}
Write-Host 'All checks passed.' -ForegroundColor Green
exit 0







