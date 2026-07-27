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

        // tech tree: 初始化节点（需在 blocks 之后）
        EFTechTree.initNodes();

        // tech tree: 构建显示树
        EFTechTree.load(EFplanets.taelosII);
    }
}
