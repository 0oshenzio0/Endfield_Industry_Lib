package endfieldindustrylib.EFcontents;

import mindustry.content.TechTree;
import mindustry.ctype.UnlockableContent;
import mindustry.type.ItemStack;
import mindustry.type.Planet;

import static endfieldindustrylib.EFcontents.EFblocks.*;
import static endfieldindustrylib.EFcontents.EFitems.*;
import static mindustry.content.TechTree.*;

/**
 * 塔卫二 (taelos-II) 科技树。
 * 注册所有物品和方块到该星球。
 *
 * 科技树分三大分支：
 *   - 集成工业系统（Automated Industry Complex）
 *   - 地区（Regions）
 *   - 物品（Items）
 *
 * 所有节点可直接点击解锁，无需消耗资源。
 * 使用 freeNode 方法自动清除方块建造需求带来的 Research 目标。
 */
public class EFTechTree {

    /** 创建无消耗、无条件的研究节点，清除方块建造依赖自动添加的 Research 目标 */
    private static TechNode freeNode(UnlockableContent content, Runnable children) {
        TechNode n = node(content, new ItemStack[0], children);
        n.objectives.clear();
        return n;
    }

    public static void load(Planet planet) {
        planet.techTree = nodeRoot("taelos-II", protocolCore, () -> {
            // ===================================================================
            // 分支一：集成工业系统
            // ===================================================================
            freeNode(automatedIndustryComplex, () -> {
                freeNode(basicIndustryPlan, () -> {
                    // ———— 基础工业一期 → 二期 → 三期 ————
                    freeNode(basicIndustryPhase1, () -> {
                        freeNode(basicIndustryPhase2, () -> {
                            freeNode(basicIndustryPhase3, () -> {});
                        });
                    });

                    // ———— 基础矿物采掘 → 中级矿物采掘 → 高级矿物采掘 ————
                    freeNode(basicMineralMining, () -> {
                        freeNode(intermediateMineralMining, () -> {
                            freeNode(advancedMineralMining, () -> {});
                        });
                    });

                    // ———— 基础运输 → 物品准入口 → 传送带分流 → 传送带跨接 → 传送带汇流 ————
                    freeNode(basicTransport, () -> {
                        freeNode(itemAccessPort, () -> {
                            freeNode(beltSplitting, () -> {
                                freeNode(beltBridging, () -> {
                                    freeNode(beltConverging, () -> {});
                                });
                            });
                        });
                    });

                    // ———— 基础精炼 → 物品塑形 → 灌装机 → 培植工艺 → 研磨工艺 ————
                    freeNode(basicRefining, () -> {
                        freeNode(itemMoulding, () -> {
                            freeNode(filler, () -> {
                                freeNode(cultivationTechnology, () -> {
                                    freeNode(grindingTech, () -> {});
                                });
                            });
                        });
                    });

                    // ———— 基础粉碎 → 零件制造 → 封装工艺 ————
                    freeNode(basicShredding, () -> {
                        freeNode(partsManufacturing, () -> {
                            freeNode(packagingTech, () -> {});
                        });
                    });

                    // ———— 基础供电 → 电力传输 → 基础发电 → 仓库存取线 ————
                    freeNode(basicPower, () -> {
                        freeNode(powerTransmission, () -> {
                            freeNode(powerGeneration, () -> {
                                freeNode(warehouseAccess, () -> {});
                            });
                        });
                    });

                    // ———— 户外储物技术 ————
                    freeNode(outdoorStorage, () -> {});
                });
            });

            // ===================================================================
            // 分支二：地区
            // ===================================================================
            freeNode(regionHub, () -> {
                freeNode(quarry, () -> {});
                freeNode(originiumResearchLab, () -> {
                    freeNode(veinSourceArea, () -> {
                        freeNode(energyHighland, () -> {});
                    });
                });
            });

            // ===================================================================
            // 分支三：物品
            // ===================================================================
            freeNode(itemsCategory, () -> {});
        });

        // 为根节点设置 planet 引用
        planet.techTree.planet = planet;

        // 将所有物品和方块注册到该星球（无科技树节点的方块也可用）
        EFitems.registerToPlanet(planet);
        EFblocks.registerToPlanet(planet);
    }
}
