package endfieldindustrylib.EFcontents;

import arc.struct.Seq;
import mindustry.ctype.UnlockableContent;
import mindustry.type.StatusEffect;

/** 科技树节点。完全依赖 Mindustry 默认图标加载。 */
public class TechTreeNode extends StatusEffect {
    public Seq<UnlockableContent> unlockables = new Seq<>();

    public TechTreeNode(String name) {
        super(name);
        hideDatabase = true;
        alwaysUnlocked = false;
        generateIcons = false;
        outline = false;
    }

    @Override
    public void onUnlock() {
        unlockables.each(UnlockableContent::quietUnlock);
    }
}
