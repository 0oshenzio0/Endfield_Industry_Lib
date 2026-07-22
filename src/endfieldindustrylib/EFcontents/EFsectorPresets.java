package endfieldindustrylib.EFcontents;

import endfieldindustrylib.EFworld.CampaignHandler;
import mindustry.type.SectorPreset;

/**
 * 塔卫二战役关卡预设。
 * <p>
 * 每个关卡对应一个扇区，使用默认的 FileMapGenerator 加载地图文件。
 * 后续需要将对应的 .msav 地图文件放入 {@code assets/maps/} 目录下，
 * 文件名需与 SectorPreset 名称一致（例如 {@code region-hub.msav}）。
 * <p>
 * 扇区坐标（构造参数中的数字）为行星网格上的位置索引，
 * 需与后续制作的 .msav 地图文件中保存的坐标一致。
 */
public class EFsectorPresets {
    public static SectorPreset
        regionHub,
        regionHubII,
        originiumResearchLab,
        veinSourceArea,
        energyHighland,
        originiumResearchLabII,
        veinSourceAreaII,
        energyHighlandII;

    public static void load() {
        // ===================================================================
        //  枢纽区（新手教程）
        // ===================================================================
        // 坐标 0 — 需地图文件: assets/maps/region-hub.msav
        regionHub = new SectorPreset("region-hub", EFplanets.taelosII, 0) {{
            // 教程关：防御战，只有几个简单的敌人，
            // 检测到玩家的防御塔数量达到一定数量后触发下一波
            alwaysUnlocked = true;
            addStartingItems = true;
            noLighting = true;
            startWaveTimeMultiplier = 3f;
            captureWave = 20;
            difficulty = 3;
            rules = r -> {
                r.winWave = captureWave;
                r.mission = "建造防御塔抵御敌人进攻";
                CampaignHandler.regionHubRules(this);
            };
        }};
        // 坐标 1 — 需地图文件: assets/maps/region-hub-ii.msav
        regionHubII = new SectorPreset("region-hub-ii", EFplanets.taelosII, 1) {{
            // 教程关：按照教程摆下几个工厂，
            // 收集五百个晶体外壳和源矿后通关
            addStartingItems = true;
            startWaveTimeMultiplier = 3f;
            difficulty = 3;
            rules = r -> {
                r.winWave = 0;
                r.mission = "收集 500 源矿与 500 紫水晶纤维";
                CampaignHandler.regionHubIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.regionHub.sector);
        }};

        // ===================================================================
        //  源石研究所
        // ===================================================================
        // 坐标 15 — 需地图文件: assets/maps/originium-research-lab.msav
        originiumResearchLab = new SectorPreset("originium-research-lab", EFplanets.taelosII, 15) {{
            // 抵御10轮敌人，保护一个单位从地图角落回到核心旁边，
            // 单位死亡则失败，一半敌人会攻击护送单位
            captureWave = 30;
            difficulty = 5;
            rules = r -> {
                r.winWave = captureWave;
                r.mission = "保护护送单位安全到达核心";
                CampaignHandler.originiumResearchLabRules(this);
            };
            shieldSectors.add(EFsectorPresets.regionHub.sector);
        }};
        // 坐标 16 — 需地图文件: assets/maps/originium-research-lab-ii.msav
        originiumResearchLabII = new SectorPreset("originium-research-lab-ii", EFplanets.taelosII, 16) {{
            // 保证某些物品产量达标，零星敌人不断涌来
            difficulty = 5;
            rules = r -> {
                r.winWave = 0;
                r.mission = "收集 300 源石外壳、200 铁锭、200 碳";
                CampaignHandler.originiumResearchLabIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.originiumResearchLab.sector);
        }};

        // ===================================================================
        //  矿脉园区
        // ===================================================================
        // 坐标 30 — 需地图文件: assets/maps/vein-source-area.msav
        veinSourceArea = new SectorPreset("vein-source-area", EFplanets.taelosII, 30) {{
            // 护送单位到达地图另一边，无核心，
            // 单位死亡即失败，敌人不断涌向目标
            difficulty = 7;
            rules = r -> {
                r.winWave = 0;
                r.mission = "护送单位穿越矿脉园区";
                CampaignHandler.veinSourceAreaRules(this);
            };
            shieldSectors.add(EFsectorPresets.originiumResearchLab.sector);
        }};
        // 坐标 31 — 需地图文件: assets/maps/vein-source-area-ii.msav
        veinSourceAreaII = new SectorPreset("vein-source-area-ii", EFplanets.taelosII, 31) {{
            // 护送单位前往三个区域破坏侵蚀核，
            // 每破坏一个侵蚀核削弱最终Boss
            difficulty = 7;
            rules = r -> {
                r.winWave = 0;
                r.mission = "摧毁三处侵蚀核，击败最终 Boss";
                CampaignHandler.veinSourceAreaIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.veinSourceArea.sector);
        }};

        // ===================================================================
        //  供能高地（最终战役）
        // ===================================================================
        // 坐标 45 — 需地图文件: assets/maps/energy-highland.msav
        energyHighland = new SectorPreset("energy-highland", EFplanets.taelosII, 45) {{
            // 限时护送单位到指定位置，
            // 挖掘侵蚀核增加倒计时，单位死亡即失败，
            // 胜利时单位同时破坏大量侵蚀核，自身牺牲
            difficulty = 9;
            rules = r -> {
                r.winWave = 0;
                r.mission = "限时护送单位到达目标位置";
                CampaignHandler.energyHighlandRules(this);
            };
            shieldSectors.add(EFsectorPresets.veinSourceArea.sector);
        }};
        // 坐标 46 — 需地图文件: assets/maps/energy-highland-ii.msav
        energyHighlandII = new SectorPreset("energy-highland-ii", EFplanets.taelosII, 46) {{
            // 摧毁地图四角炮台→Boss出现→击败Boss胜利
            difficulty = 9;
            isLastSector = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "摧毁四角炮台，击败最终 Boss";
                CampaignHandler.energyHighlandIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.energyHighland.sector);
        }};
    }
}
