package endfieldindustrylib;

import endfieldindustrylib.EFcontents.EFblocks;
import endfieldindustrylib.EFcontents.EFitems;
import endfieldindustrylib.EFcontents.EFplanets;
import endfieldindustrylib.EFcontents.EFTechTree;

public class Lib extends mindustry.mod.Mod {

    @Override
    public void loadContent() {
        // item
        EFitems.load();

        // planet (塔卫二)
        EFplanets.loadContents();

        // block
        EFblocks.load();

        // tech tree (需在物品和方块都加载完成后)
        EFTechTree.load(EFplanets.taelosII);
    }
}
