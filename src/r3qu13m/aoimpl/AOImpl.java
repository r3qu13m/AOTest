package r3qu13m.aoimpl;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import r3qu13m.aoimpl.block.BlockAOTest;
import r3qu13m.aoimpl.client.RenderAOTest;
import r3qu13m.xorlib.api.XORSide;
import r3qu13m.xorlib.api.util.XORUtil;

@Mod(name = "AOImpl", version = "%%VERSION%%", modid = "AOImpl", dependencies = "required-after:XORLib|main")
public class AOImpl {

	@Mod.Instance("AOImpl")
	public static AOImpl instance;

	public int idRenderAOTest = RenderingRegistry.getNextAvailableRenderId();
	public Block blockAOTest;

	@SideOnly(Side.CLIENT)
	public void initClient() {
		RenderingRegistry.registerBlockHandler(new RenderAOTest());
	}

	@Mod.Init
	public void init(final FMLInitializationEvent event) {
		this.blockAOTest = new BlockAOTest(1623);
		XORUtil.addNameForObject(this.blockAOTest, "AOTest", "AOTest");
		GameRegistry.registerBlock(this.blockAOTest, this.blockAOTest.getBlockName());
		if (XORSide.isClient()) {
			this.initClient();
		}
	}
}
