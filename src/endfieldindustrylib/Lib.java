package endfieldindustrylib;

import endfieldindustrylib.EFcontents.EFblocks;
import endfieldindustrylib.EFcontents.EFitems;
import endfieldindustrylib.EFcontents.EFplanets;

public class Lib extends mindustry.mod.Mod {

    @Override
    public void loadContent() {
        // item
        EFitems.load();

        EFplanets.loadContents();

        // block
        EFblocks.load();
    }
}