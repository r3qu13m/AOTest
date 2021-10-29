package r3qu13m.aoimpl.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import r3qu13m.aoimpl.AOImpl;

public class BlockAOTest extends Block {
	public BlockAOTest(final int id) {
		super(id, Material.rock);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setBlockName("aotest");
		this.setTextureFile("/terrain.png");
		this.blockIndexInTexture = 3;
	}

	@Override
	public int getBlockTextureFromSide(final int side) {
		return 6;
	}

	@Override
	public int getRenderType() {
		return AOImpl.instance.idRenderAOTest;
	}
}
