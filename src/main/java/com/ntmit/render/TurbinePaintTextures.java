package com.ntmit.render;

import com.ntmit.lib.RefStrings;
import com.ntmit.lib.TurbinePaintAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TurbinePaintTextures {

	private static final Set<String> EXCLUDED_PARTS = new HashSet<String>();
	static {
		EXCLUDED_PARTS.add("Shaft");
		EXCLUDED_PARTS.add("Blades");
		EXCLUDED_PARTS.add("Core");
	}

	private static final ResourceLocation EXPORT_OBJ = new ResourceLocation("leafia",
			"textures/_integrated/machines/modular_turbines/export.obj");
	private static final ResourceLocation MASK_TEXTURE1 = new ResourceLocation(RefStrings.MODID,
			"textures/turbine_paint/texture1_paintmask.png");
	private static final ResourceLocation MASK_GENERATOR = new ResourceLocation(RefStrings.MODID,
			"textures/turbine_paint/generator3x3_paintmask.png");
	private static final float GLOBAL_REFERENCE_LUMINANCE = 96.0F;
	private static final float FLYWHEEL_LIGHT = 0.539F;

	private static final Map<String, BufferedImage> BASE_IMAGES = new HashMap<String, BufferedImage>();
	private static final Map<String, BufferedImage> MASKS = new HashMap<String, BufferedImage>();
	private static final Map<String, ResourceLocation> PAINTED = new HashMap<String, ResourceLocation>();
	private static final Map<String, PartUv> PART_UVS = new HashMap<String, PartUv>();
	private static final Map<Class<?>, Field> CORE_FIELDS = new HashMap<Class<?>, Field>();
	private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<Context>();
	private static boolean partUvsLoaded;

	private TurbinePaintTextures() {
	}

	public static void begin(int color) {
		Context previous = CONTEXT.get();
		Context context = new Context();
		context.color = color;
		context.base = previous == null ? null : previous.base;
		context.previous = previous;
		CONTEXT.set(context);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void end() {
		Context context = CONTEXT.get();
		if (context == null) return;
		if (context.previous == null) {
			CONTEXT.remove();
		} else {
			CONTEXT.set(context.previous);
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void recordBase(ResourceLocation base) {
		if (base == null || !isTurbineTexture(base)) return;
		Context context = CONTEXT.get();
		if (context != null) context.base = base;
	}

	private static boolean isTurbineTexture(ResourceLocation texture) {
		if (texture == null) return false;
		String path = texture.getPath();
		return !path.startsWith("dynamic/turbine/");
	}

	public static void preparePart(String part) {
		Context context = CONTEXT.get();
		if (context == null || context.base == null) return;
		if (EXCLUDED_PARTS.contains(part)) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(context.base);
			return;
		}
		ResourceLocation painted = context.color < 0
				? null
				: painted(context.base, part, context.color);
		Minecraft.getMinecraft().getTextureManager().bindTexture(
				painted == null ? context.base : painted);
	}

	public static void prepareAll() {
		preparePart("*");
	}

	public static void applyFlywheelColor(float red, float green, float blue) {
		if (red == 1.0F && green == 1.0F && blue == 1.0F) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			return;
		}
		Context context = CONTEXT.get();
		if (context == null || context.color < 0) {
			GlStateManager.color(red, green, blue, 1.0F);
			return;
		}
		float factor = Math.max(red, Math.max(green, blue)) / FLYWHEEL_LIGHT;
		GlStateManager.color(clamp(((context.color >> 16) & 0xFF) / 255.0F * factor),
				clamp(((context.color >> 8) & 0xFF) / 255.0F * factor),
				clamp((context.color & 0xFF) / 255.0F * factor), 1.0F);
	}

	public static int colorOf(Object tile) {
		int local = TurbinePaintAccess.NO_PAINT;
		if (tile instanceof TurbinePaintAccess) {
			local = ((TurbinePaintAccess) tile).ntmit$getPaintColor();
			if (local >= 0) return local;
		}
		if (!(tile instanceof TileEntity)) return TurbinePaintAccess.NO_PAINT;
		try {
			Class<?> type = tile.getClass();
			Field field = CORE_FIELDS.get(type);
			if (field == null) {
				field = type.getField("core");
				CORE_FIELDS.put(type, field);
			}
			Object core = field.get(tile);
			if (core instanceof TurbinePaintAccess) {
				return ((TurbinePaintAccess) core).ntmit$getPaintColor();
			}
		} catch (Throwable throwable) {
		}
		return TurbinePaintAccess.NO_PAINT;
	}

	private static float clamp(float value) {
		return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
	}

	private static final class Context {
		int color;
		ResourceLocation base;
		Context previous;
	}

	private static final class PartUv {
		float minU = 1.0F;
		float maxU = 0.0F;
		float minV = 1.0F;
		float maxV = 0.0F;
		boolean valid;

		void include(float u, float v) {
			if (u < minU) minU = u;
			if (u > maxU) maxU = u;
			if (v < minV) minV = v;
			if (v > maxV) maxV = v;
			valid = true;
		}
	}
	private static ResourceLocation painted(ResourceLocation base, String part, int color) {
		String key = base + "|" + part + "|" + color;
		TextureManager manager = Minecraft.getMinecraft().getTextureManager();
		ResourceLocation cached = PAINTED.get(key);
		if (cached != null && manager.getTexture(cached) != null) return cached;
		try {
			BufferedImage source = baseImage(base);
			if (source == null) return null;
			PartUv uv = partUv(part);
			if (uv == null || !uv.valid) uv = partUv("*");
			if (uv == null || !uv.valid) return null;
			BufferedImage mask = maskFor(base);
			float max = partMax(base, part, source, mask, uv);
			if (max <= 0.0F) return null;
			int width = source.getWidth();
			int height = source.getHeight();
			BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int paintR = (color >> 16) & 0xFF;
			int paintG = (color >> 8) & 0xFF;
			int paintB = color & 0xFF;
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					int pixel = source.getRGB(x, y);
					int alpha = (pixel >> 24) & 0xFF;
					if (alpha == 0 || forbidden(mask, x, y)) {
						target.setRGB(x, y, pixel);
						continue;
					}
					int red = (pixel >> 16) & 0xFF;
					int green = (pixel >> 8) & 0xFF;
					int blue = pixel & 0xFF;
					float brightness = Math.min(1.0F, luminance(red, green, blue) / max);
					target.setRGB(x, y, pack(alpha,
							Math.round(paintR * brightness),
							Math.round(paintG * brightness),
							Math.round(paintB * brightness)));
				}
			}
			DynamicTexture texture = new DynamicTexture(target);
			ResourceLocation location = new ResourceLocation(RefStrings.MODID,
					"dynamic/turbine/" + Integer.toHexString(key.hashCode()));
			manager.loadTexture(location, texture);
			PAINTED.put(key, location);
			return location;
		} catch (Throwable throwable) {
			return null;
		}
	}

	private static BufferedImage baseImage(ResourceLocation location) {
		String key = location.toString();
		BufferedImage image = BASE_IMAGES.get(key);
		if (image != null) return image;
		image = loadImage(location);
		if (image != null) BASE_IMAGES.put(key, image);
		return image;
	}

	private static BufferedImage maskFor(ResourceLocation base) {
		String path = base.getPath();
		if (path.endsWith("/texture1.png")) return mask(MASK_TEXTURE1);
		if (path.endsWith("/generator3x3.png")) return mask(MASK_GENERATOR);
		return null;
	}

	private static BufferedImage mask(ResourceLocation location) {
		String key = location.toString();
		BufferedImage image = MASKS.get(key);
		if (image != null) return image;
		image = loadImage(location);
		if (image != null) MASKS.put(key, image);
		return image;
	}

	private static BufferedImage loadImage(ResourceLocation location) {
		try {
			IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
			IResource resource = resourceManager.getResource(location);
			try (InputStream stream = resource.getInputStream()) {
				return ImageIO.read(stream);
			}
		} catch (Throwable throwable) {
			return null;
		}
	}

	private static float partMax(ResourceLocation base, String part, BufferedImage source, BufferedImage mask, PartUv uv) {
		return GLOBAL_REFERENCE_LUMINANCE;
	}

	private static boolean forbidden(BufferedImage mask, int x, int y) {
		if (mask == null) return false;
		if (x < 0 || y < 0 || x >= mask.getWidth() || y >= mask.getHeight()) return false;
		int pixel = mask.getRGB(x, y);
		return ((pixel >> 16) & 0xFF) < 128
				&& ((pixel >> 8) & 0xFF) < 128
				&& (pixel & 0xFF) < 128;
	}

	private static float luminance(int red, int green, int blue) {
		return 0.299F * red + 0.587F * green + 0.114F * blue;
	}

	private static int pack(int alpha, int red, int green, int blue) {
		return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}

	private static PartUv partUv(String part) {
		if ("*".equals(part)) {
			PartUv all = new PartUv();
			all.include(0.0F, 0.0F);
			all.include(1.0F, 1.0F);
			return all;
		}
		loadPartUvs();
		return PART_UVS.get(part);
	}

	private static void loadPartUvs() {
		if (partUvsLoaded) return;
		partUvsLoaded = true;
		try {
			PART_UVS.putAll(parseObj(EXPORT_OBJ));
		} catch (Throwable throwable) {
		}
	}

	private static Map<String, PartUv> parseObj(ResourceLocation location) throws java.io.IOException {
		Map<String, PartUv> result = new HashMap<String, PartUv>();
		java.util.ArrayList<float[]> uvs = new java.util.ArrayList<float[]>();
		String current = null;
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		IResource resource = resourceManager.getResource(location);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("o ") || line.startsWith("g ")) {
					current = line.substring(2).trim();
					ensurePart(result, current);
				} else if (line.startsWith("vt ")) {
					String[] tokens = line.split("\\s+");
					if (tokens.length >= 3) {
						try {
							uvs.add(new float[]{Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])});
						} catch (NumberFormatException exception) {
						}
					}
				} else if (line.startsWith("f ")) {
					if (current == null) continue;
					PartUv part = ensurePart(result, current);
					String[] tokens = line.split("\\s+");
					for (int i = 1; i < tokens.length; i++) {
						String[] elements = tokens[i].split("/");
						if (elements.length < 2 || elements[1].isEmpty()) continue;
						try {
							int index = Integer.parseInt(elements[1]) - 1;
							if (index < 0 || index >= uvs.size()) continue;
							float[] uv = uvs.get(index);
							part.include(uv[0], uv[1]);
						} catch (NumberFormatException exception) {
						}
					}
				}
			}
		} catch (Throwable throwable) {
		}
		return result;
	}

	private static PartUv ensurePart(Map<String, PartUv> parts, String name) {
		PartUv part = parts.get(name);
		if (part == null) {
			part = new PartUv();
			parts.put(name, part);
		}
		return part;
	}
}

