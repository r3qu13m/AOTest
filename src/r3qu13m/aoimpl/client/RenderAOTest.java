package r3qu13m.aoimpl.client;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;
import r3qu13m.aoimpl.AOImpl;
import r3qu13m.xorlib.api.util.Pair;

@SideOnly(Side.CLIENT)
public class RenderAOTest implements ISimpleBlockRenderingHandler {
	private double renderMinX;
	private double renderMinY;
	private double renderMinZ;
	private double renderMaxX;
	private double renderMaxY;
	private double renderMaxZ;
	private AxisAlignedBB renderAABB;
	private boolean renderAllSide;
	private float colorTopLeftR;
	private float colorTopLeftG;
	private float colorTopLeftB;
	private float colorTopRightR;
	private float colorTopRightG;
	private float colorTopRightB;
	private float colorBotLeftR;
	private float colorBotLeftG;
	private float colorBotLeftB;
	private float colorBotRightR;
	private float colorBotRightG;
	private float colorBotRightB;
	private int brightnessTopLeft;
	private int brightnessTopRight;
	private int brightnessBotLeft;
	private int brightnessBotRight;
	private boolean enableAO;

	// UV座標系におけるテクスチャ一辺の大きさ
	// 256x256の場合は16[px]に相当する値
	private final static double TEXTURE_UV_SIZE = 16D / 256D;

	private Pair<Double, Double> computeUV(final int textureIndex) {
		return Pair.create(((textureIndex & 15) << 4) / 256D, ((textureIndex >> 4) << 4) / 256D);
	}

	@Override
	public void renderInventoryBlock(final Block block, final int metadata, final int modelID,
			final RenderBlocks renderer) {
		final Tessellator tes = Tessellator.instance;
		int color = block.getRenderColor(metadata);

		this.setRenderBounds(block);
		this.bindTexture(block.getTextureFile());
		if (block.blockID == Block.grass.blockID) {
			color = 0xffffff;
		}

		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glTranslated(-0.5, -0.5, -0.5);

		tes.startDrawingQuads();

		for (final ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			this.setColor(block, color, dir);
			this.renderSide(this.renderAABB, block.getBlockTextureFromSideAndMetadata(dir.ordinal(), metadata), dir);
		}

		tes.draw();

		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}

	@Override
	public boolean renderWorldBlock(final IBlockAccess world, final int x, final int y, final int z, final Block block,
			final int modelId, final RenderBlocks renderer) {
		final Tessellator tes = Tessellator.instance;
		final AxisAlignedBB renderBox = this.renderAABB.offset(x, y, z);

		this.enableAO = Minecraft.isAmbientOcclusionEnabled();
		this.setRenderBounds(block);
		this.bindTexture(block.getTextureFile());

		// 乗算色計算
		final int color = block.colorMultiplier(world, x, y, z);

		// 各面の明るさ計算に用いる座標のオフセット決定に用いる条件群
		final boolean[] specificConditions = { this.renderMinY <= 0, this.renderMaxY >= 1, this.renderMinZ <= 0,
				this.renderMaxZ >= 1, this.renderMinX <= 0, this.renderMaxX >= 1, };

		// 各面描画
		for (final ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			if (this.renderAllSide || block.shouldSideBeRendered(world, x + dir.offsetX, y + dir.offsetY,
					z + dir.offsetZ, dir.ordinal())) {
				this.setColor(block, color, dir);

				if (this.enableAO) {
					this.computeAO(block, world, x, y, z, dir, specificConditions[dir.ordinal()]);
				} else if (specificConditions[dir.ordinal()]) {
					this.brightnessTopLeft = this.brightnessTopRight = this.brightnessBotLeft = this.brightnessBotRight = block
							.getMixedBrightnessForBlock(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
				}

				this.renderSide(renderBox, block.getBlockTexture(world, x, y, z, dir.ordinal()), dir);
			}
		}

		return true;
	}

	public void renderSide(final AxisAlignedBB box, final int textureIndex, final ForgeDirection dir) {
		switch (dir) {
		case DOWN:
			this.renderDown(box, textureIndex);
			break;
		case EAST:
			this.renderEast(box, textureIndex);
			break;
		case NORTH:
			this.renderNorth(box, textureIndex);
			break;
		case SOUTH:
			this.renderSouth(box, textureIndex);
			break;
		case UP:
			this.renderUp(box, textureIndex);
			break;
		case WEST:
			this.renderWest(box, textureIndex);
			break;
		default:
			break;
		}
	}

	public void setRenderBounds(final Block block) {
		this.renderMinX = block.getBlockBoundsMinX();
		this.renderMinY = block.getBlockBoundsMinY();
		this.renderMinZ = block.getBlockBoundsMinZ();
		this.renderMaxX = block.getBlockBoundsMaxX();
		this.renderMaxY = block.getBlockBoundsMaxY();
		this.renderMaxZ = block.getBlockBoundsMaxZ();
		this.renderAABB = AxisAlignedBB.getBoundingBox(this.renderMinX, this.renderMinY, this.renderMinZ,
				this.renderMaxX, this.renderMaxY, this.renderMaxZ);
	}

	// side = 0 (DOWN) 法線: (0.5, -2, 0.5)
	public void renderDown(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.maxX, box.minY, box.maxZ, maxU, maxV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.minX, box.minY, box.maxZ, minU, maxV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.minX, box.minY, box.minZ, minU, minV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.maxX, box.minY, box.minZ, maxU, minV);
	}

	// side = 1 (UP) 法線: (0.5, 2, 0.5)
	public void renderUp(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.maxZ, maxU, maxV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.minZ, maxU, minV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.minX, box.maxY, box.minZ, minU, minV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.minX, box.maxY, box.maxZ, minU, maxV);
	}

	// side = 2 (NORTH) 法線: (0.5, 0.5, -2)
	public void renderNorth(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.maxX, box.minY, box.minZ, maxU, maxV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.minX, box.minY, box.minZ, minU, maxV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.minX, box.maxY, box.minZ, minU, minV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.minZ, maxU, minV);
	}

	// side = 3 (SOUTH) 法線: (0.5, 0.5, 2)
	public void renderSouth(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.maxX, box.minY, box.maxZ, maxU, maxV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.maxZ, maxU, minV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.minX, box.maxY, box.maxZ, minU, minV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.minX, box.minY, box.maxZ, minU, maxV);
	}

	// side = 4 (WEST) 法線: (-2, 0.5, 0.5)
	public void renderWest(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.minX, box.minY, box.maxZ, maxU, maxV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.minX, box.maxY, box.maxZ, maxU, minV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.minX, box.maxY, box.minZ, minU, minV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.minX, box.minY, box.minZ, minU, maxV);
	}

	// side = 5 (EAST) 法線: (2, 0.5, 0.5)
	public void renderEast(final AxisAlignedBB box, final int textureIndex) {
		final Tessellator tes = Tessellator.instance;

		final Pair<Double, Double> uv = this.computeUV(textureIndex);
		final double minU = uv.first;
		final double maxU = uv.first + RenderAOTest.TEXTURE_UV_SIZE;
		final double minV = uv.second;
		final double maxV = uv.second + RenderAOTest.TEXTURE_UV_SIZE;

		tes.setBrightness(this.brightnessBotRight);
		tes.setColorOpaque_F(this.colorBotRightR, this.colorBotRightG, this.colorBotRightB);
		tes.addVertexWithUV(box.maxX, box.minY, box.maxZ, maxU, maxV);

		tes.setBrightness(this.brightnessBotLeft);
		tes.setColorOpaque_F(this.colorBotLeftR, this.colorBotLeftG, this.colorBotLeftB);
		tes.addVertexWithUV(box.maxX, box.minY, box.minZ, minU, maxV);

		tes.setBrightness(this.brightnessTopLeft);
		tes.setColorOpaque_F(this.colorTopLeftR, this.colorTopLeftG, this.colorTopLeftB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.minZ, minU, minV);

		tes.setBrightness(this.brightnessTopRight);
		tes.setColorOpaque_F(this.colorTopRightR, this.colorTopRightG, this.colorTopRightB);
		tes.addVertexWithUV(box.maxX, box.maxY, box.maxZ, maxU, minV);
	}

	@Override
	public boolean shouldRender3DInInventory() {
		return true;
	}

	@Override
	public int getRenderId() {
		return AOImpl.instance.idRenderAOTest;
	}

	private float getAmbientOcclusionLightValue(final IBlockAccess var1, final int var2, final int var3,
			final int var4) {
		// borrow from ShadersMod
		final Block var5 = Block.blocksList[var1.getBlockId(var2, var3, var4)];
		return var5 == null ? 1.0F
				: (var5.getClass() == BlockGlass.class ? 1.0F
						: (var5.blockMaterial.blocksMovement() && var5.renderAsNormalBlock() ? 0.2F : 1.0F));
	}

	private int getAoBrightness(final int brightnessMain, int brightnessSide1, int brightnessSide2,
			int brightnessCorner) {
		// borrow from ShadersMod
		if (brightnessSide1 == 0) {
			brightnessSide1 = brightnessMain;
		}

		if (brightnessSide2 == 0) {
			brightnessSide2 = brightnessMain;
		}

		if (brightnessCorner == 0) {
			brightnessCorner = brightnessMain;
		}

		return brightnessSide1 + brightnessSide2 + brightnessCorner + brightnessMain >> 2 & 16711935;
	}

	private void computeAO(final Block block, final IBlockAccess world, final int x, final int y, final int z,
			final ForgeDirection dir, final boolean doAddOffset) {
		final int posX = x + (doAddOffset ? dir.offsetX : 0);
		final int posY = y + (doAddOffset ? dir.offsetY : 0);
		final int posZ = z + (doAddOffset ? dir.offsetZ : 0);
		ForgeDirection aboveDir, belowDir, leftDir, rightDir;

		if (dir == ForgeDirection.DOWN || dir == ForgeDirection.UP) {
			/* xz plane
			 *
			 *       -z
			 *        ^
			 *        |
			 * -x < - + - > +x
			 *        |
			 *        V
			 *       +z
			 *
			 */
			aboveDir = ForgeDirection.NORTH;
			belowDir = ForgeDirection.SOUTH;
			leftDir = ForgeDirection.WEST;
			rightDir = ForgeDirection.EAST;
		} else if (dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH) {
			/* xy plane
			 *
			 *       +y
			 *        ^
			 *        |
			 * -x < - + - > +x
			 *        |
			 *        V
			 *       -y
			 *
			 */
			aboveDir = ForgeDirection.UP;
			belowDir = ForgeDirection.DOWN;
			leftDir = ForgeDirection.WEST;
			rightDir = ForgeDirection.EAST;
		} else { // WEST, EAST
			/* yz plane
			 *
			 *       +y
			 *        ^
			 *        |
			 * -z < - + - > +z
			 *        |
			 *        V
			 *       -y
			 *
			 */
			aboveDir = ForgeDirection.UP;
			belowDir = ForgeDirection.DOWN;
			leftDir = ForgeDirection.NORTH;
			rightDir = ForgeDirection.SOUTH;
		}

		/*
		 * 0 | 1 | 2
		 * 3 | 4 | 5
		 * 6 | 7 | 8
		 *
		 * 5 = (aoMain, brightnessMain)
		 *
		 * (main, side1, side2, corner)
		 *
		 * bottom left vertex -> [3, 4, 6, 7]: (4, 3, 7, 6)
		 * bottom right vertex -> [4, 5, 7, 8]: (4, 5, 7, 8)
		 * top left vertex -> [0, 1, 3, 4]: (4, 1, 3, 0)
		 * top right vertex -> [1, 2, 4, 5]: (4, 1, 5, 2)
		 */
		final ChunkCoordinates params[] = new ChunkCoordinates[9];
		// center
		params[4] = new ChunkCoordinates(posX, posY, posZ);

		params[0] = this.addDirectionToVec(params[4], aboveDir, leftDir);
		params[1] = this.addDirectionToVec(params[4], aboveDir);
		params[2] = this.addDirectionToVec(params[4], aboveDir, rightDir);
		params[3] = this.addDirectionToVec(params[4], leftDir);
		params[5] = this.addDirectionToVec(params[4], rightDir);
		params[6] = this.addDirectionToVec(params[4], belowDir, leftDir);
		params[7] = this.addDirectionToVec(params[4], belowDir);
		params[8] = this.addDirectionToVec(params[4], belowDir, rightDir);

		final Pair<Integer, Float> botLeft = this.computeAO_do(block, world, params[4], params[3], params[7],
				params[6]);
		this.brightnessBotLeft = botLeft.first;
		this.colorBotLeftR *= botLeft.second;
		this.colorBotLeftG *= botLeft.second;
		this.colorBotLeftB *= botLeft.second;

		final Pair<Integer, Float> botRight = this.computeAO_do(block, world, params[4], params[5], params[7],
				params[8]);
		this.brightnessBotRight = botRight.first;
		this.colorBotRightR *= botRight.second;
		this.colorBotRightG *= botRight.second;
		this.colorBotRightB *= botRight.second;

		final Pair<Integer, Float> topLeft = this.computeAO_do(block, world, params[4], params[1], params[3],
				params[0]);
		this.brightnessTopLeft = topLeft.first;
		this.colorTopLeftR *= topLeft.second;
		this.colorTopLeftG *= topLeft.second;
		this.colorTopLeftB *= topLeft.second;

		final Pair<Integer, Float> topRight = this.computeAO_do(block, world, params[4], params[1], params[5],
				params[2]);
		this.brightnessTopRight = topRight.first;
		this.colorTopRightR *= topRight.second;
		this.colorTopRightG *= topRight.second;
		this.colorTopRightB *= topRight.second;
	}

	private Pair<Integer, Float> computeAO_do(final Block block, final IBlockAccess world,
			final ChunkCoordinates mainVec, final ChunkCoordinates sideVec1, final ChunkCoordinates sideVec2,
			final ChunkCoordinates cornerVec) {
		// TODO: キャッシュ
		final int idMain = world.getBlockId(mainVec.posX, mainVec.posY, mainVec.posZ);
		final int idSide1 = world.getBlockId(sideVec1.posX, sideVec1.posY, sideVec1.posZ);
		final int idSide2 = world.getBlockId(sideVec2.posX, sideVec2.posY, sideVec2.posZ);
		final int idCorner = world.getBlockId(cornerVec.posX, cornerVec.posY, cornerVec.posZ);
		final int brightnessMain = block.getMixedBrightnessForBlock(world, mainVec.posX, mainVec.posY, mainVec.posZ);
		final float aoMain = this.getAmbientOcclusionLightValue(world, mainVec.posX, mainVec.posY, mainVec.posZ);

		final int brightnessSide1 = block.getMixedBrightnessForBlock(world, sideVec1.posX, sideVec1.posY,
				sideVec1.posZ);
		final float aoSide1 = this.getAmbientOcclusionLightValue(world, sideVec1.posX, sideVec1.posY, sideVec1.posZ);

		final int brightnessSide2 = block.getMixedBrightnessForBlock(world, sideVec2.posX, sideVec2.posY,
				sideVec2.posZ);
		final float aoSide2 = this.getAmbientOcclusionLightValue(world, sideVec2.posX, sideVec2.posY, sideVec2.posZ);

		int brightnessCorner;
		float aoCorner;

		if (!Block.canBlockGrass[idSide1] && !Block.canBlockGrass[idSide2]) {
			// 両方埋まっている場合は角を無視
			brightnessCorner = brightnessSide1;
			aoCorner = aoSide1;
		} else {
			brightnessCorner = block.getMixedBrightnessForBlock(world, cornerVec.posX, cornerVec.posY, cornerVec.posZ);
			aoCorner = this.getAmbientOcclusionLightValue(world, cornerVec.posX, cornerVec.posY, cornerVec.posZ);
		}

		// 平均値
		final int brightness = this.getAoBrightness(brightnessMain, brightnessSide1, brightnessSide2, brightnessCorner);
		final float ao = (aoMain + aoSide1 + aoSide2 + aoCorner) / 4F;

		return Pair.create(brightness, ao);
	}

	private void setColor(final Block block, final int color, final ForgeDirection dir) {
		final float baseColorR = ((color >> 16) & 255) / 256F;
		final float baseColorG = ((color >> 8) & 255) / 256F;
		final float baseColorB = (color & 255) / 256F;
		final float botColorFactor = 0.5F;
		final float topColorFactor = 1.0F;
		final float weColorFactor = 0.8F;
		final float nsColorFactor = 0.6F;

		float r, g, b;

		if (dir == ForgeDirection.DOWN) {
			r = g = b = botColorFactor;
		} else if (dir == ForgeDirection.UP) {
			r = g = b = topColorFactor;
		} else if (dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH) {
			r = g = b = nsColorFactor;
		} else { // WEST, EAST
			r = g = b = weColorFactor;
		}

		if (block.blockID != Block.grass.blockID || dir == ForgeDirection.UP) {
			r *= baseColorR;
			g *= baseColorG;
			b *= baseColorB;
		}

		this.colorTopLeftR = this.colorTopRightR = this.colorBotLeftR = this.colorBotRightR = r;
		this.colorTopLeftG = this.colorTopRightG = this.colorBotLeftG = this.colorBotRightG = g;
		this.colorTopLeftB = this.colorTopRightB = this.colorBotLeftB = this.colorBotRightB = b;
	}

	private ChunkCoordinates addDirectionToVec(final ChunkCoordinates baseVec, final ForgeDirection... dirs) {
		int offsetX = 0, offsetY = 0, offsetZ = 0;
		for (final ForgeDirection dir : dirs) {
			offsetX += dir.offsetX;
			offsetY += dir.offsetY;
			offsetZ += dir.offsetZ;
		}
		return new ChunkCoordinates(baseVec.posX + offsetX, baseVec.posY + offsetY, baseVec.posZ + offsetZ);
	}

	private void bindTexture(final String path) {
		final RenderEngine engine = Minecraft.getMinecraft().renderEngine;
		engine.bindTexture(engine.getTexture(path));
	}
}
