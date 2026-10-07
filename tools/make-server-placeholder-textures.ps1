<#
.SYNOPSIS
    Generates the placeholder textures of the three server column families.

.DESCRIPTION
    SERVER_PLAN.md §9 P0 asks for placeholder textures for the three columns (core, compute, heat
    exchanger), four of them each: front, side, top and bottom - twelve files in total. They are flat
    16x16 plates with one accent colour per family and one pattern per cell kind, just enough to tell
    the families and the block sides apart in game while the real art does not exist yet.

    Files land in the mod's own assets folder and, when the dev resource pack is present, in that
    pack as well - so F3+T is enough to look at a change made here (see README.md, "Workflow that
    saved time").

.EXAMPLE
    cd 'D:\new mod\tools'
    .\make-server-placeholder-textures.ps1
#>
[CmdletBinding()]
param(
	[string]$ProjectRoot
)

# $PSScriptRoot is not populated yet while the parameter defaults are evaluated, so the project root
# is resolved here instead of in the param block.
if ([string]::IsNullOrEmpty($ProjectRoot)) {
	$ProjectRoot = Split-Path -Parent $PSScriptRoot
}

if (-not (Test-Path (Join-Path $ProjectRoot 'build.gradle'))) {
	throw "'$ProjectRoot' does not look like the NTM-IT project root (no build.gradle in it)."
}

Add-Type -AssemblyName System.Drawing

$families = [ordered]@{
	'server_core'           = @{ base = '#6E7A86'; accent = '#4FC3D9' }   # steel grey  + cyan
	'server_compute'        = @{ base = '#5E6B5E'; accent = '#6FD07A' }   # green grey  + green
	'server_heat_exchanger' = @{ base = '#7A6A5C'; accent = '#E08A3C' }   # warm grey   + orange
}

$kinds = 'front', 'side', 'top', 'bottom'

$targets = @(
	(Join-Path $ProjectRoot 'src\main\resources\assets\ntm-it\textures\blocks'),
	(Join-Path $ProjectRoot 'run\resourcepacks\ntmit-dev\assets\ntm-it\textures\blocks')
)

function Get-Shade([string]$hex, [double]$factor) {
	$c = [System.Drawing.ColorTranslator]::FromHtml($hex)
	$r = [Math]::Min(255, [Math]::Max(0, [int]($c.R * $factor)))
	$g = [Math]::Min(255, [Math]::Max(0, [int]($c.G * $factor)))
	$b = [Math]::Min(255, [Math]::Max(0, [int]($c.B * $factor)))
	return [System.Drawing.Color]::FromArgb($r, $g, $b)
}

function New-PlaceholderTexture([string]$path, [hashtable]$palette, [string]$kind) {
	$base   = Get-Shade $palette.base 1.00
	$mid    = Get-Shade $palette.base 0.80
	$dark   = Get-Shade $palette.base 0.55
	$light  = Get-Shade $palette.base 1.22
	$accent = [System.Drawing.ColorTranslator]::FromHtml($palette.accent)

	$bmp = New-Object System.Drawing.Bitmap 16, 16
	$gfx = [System.Drawing.Graphics]::FromImage($bmp)
	$gfx.Clear($base)

	$bDark   = New-Object System.Drawing.SolidBrush $dark
	$bMid    = New-Object System.Drawing.SolidBrush $mid
	$bLight  = New-Object System.Drawing.SolidBrush $light
	$bAccent = New-Object System.Drawing.SolidBrush $accent
	$pen     = New-Object System.Drawing.Pen $dark, 1

	# common plate: a slightly lighter inner plate with a dark frame around it
	$gfx.FillRectangle($bMid, 1, 1, 14, 14)
	$gfx.DrawRectangle($pen, 0, 0, 15, 15)

	switch ($kind) {
		'front' {
			# the face the player looks at: a dark screen with an accent stripe underneath
			$gfx.FillRectangle($bDark, 3, 3, 10, 4)
			$gfx.FillRectangle($bAccent, 4, 8, 8, 2)
			$gfx.FillRectangle($bLight, 3, 11, 10, 1)
		}
		'side' {
			# two grooves, so a side face is recognisable when a column is looked at from the side
			$gfx.FillRectangle($bDark, 4, 2, 1, 12)
			$gfx.FillRectangle($bDark, 11, 2, 1, 12)
			$gfx.FillRectangle($bLight, 2, 2, 1, 12)
		}
		'top' {
			# the lid: a raised plate with a bolt in each corner
			$gfx.FillRectangle($bDark, 2, 2, 12, 12)
			$gfx.FillRectangle($bLight, 4, 4, 8, 8)
			foreach ($x in 2, 12) { foreach ($y in 2, 12) { $gfx.FillRectangle($bAccent, $x, $y, 2, 2) } }
		}
		'bottom' {
			# the underside: the darkest of the four, with four feet
			$gfx.Clear($dark)
			$gfx.FillRectangle($bMid, 2, 2, 12, 12)
			$gfx.FillRectangle($bDark, 3, 3, 3, 3)
			$gfx.FillRectangle($bDark, 10, 3, 3, 3)
			$gfx.FillRectangle($bDark, 3, 10, 3, 3)
			$gfx.FillRectangle($bDark, 10, 10, 3, 3)
		}
	}

	# one accent marker in the bottom left corner, i.e. the family signature
	$gfx.FillRectangle($bAccent, 1, 13, 2, 2)

	$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)

	$pen.Dispose(); $bDark.Dispose(); $bMid.Dispose(); $bLight.Dispose(); $bAccent.Dispose()
	$gfx.Dispose(); $bmp.Dispose()
}

foreach ($family in $families.Keys) {
	foreach ($kind in $kinds) {
		$file = "${family}_${kind}.png"

		foreach ($dir in $targets) {
			# the dev resource pack is optional: without it the generated assets still ship in the mod
			if (-not (Test-Path $dir)) {
				if ($dir -like '*resourcepacks*') { continue }
				New-Item -ItemType Directory -Path $dir -Force | Out-Null
			}

			New-PlaceholderTexture (Join-Path $dir $file) $families[$family] $kind
			Write-Host "wrote $(Join-Path $dir $file)"
		}
	}
}
