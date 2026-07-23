package endfieldindustrylib.EFcontents;

import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;
import mindustry.type.ItemStack;
import mindustry.type.Planet;

import static endfieldindustrylib.EFcontents.EFblocks.*;
import static endfieldindustrylib.EFcontents.EFitems.*;
import static mindustry.content.TechTree.*;

/**
 * 塔卫二 (taelos-II) 科技树。
 * 使用 TechTreeNode 自定义节点，支持：
 * - 节点名称和描述（bundle 文件配置）
 * - 节点花费（研究消耗）
 * - 解锁关联的多个内容（unlockables）
 */
public class EFTechTree {

    private static TechNode freeNode(UnlockableContent content, Runnable children) {
        TechNode n = node(content, new ItemStack[0], children);
        n.objectives.clear();
        return n;
    }

    private static TechNode orphanNode(UnlockableContent content, ItemStack[] requirements) {
        TechNode n = new TechNode(null, content, requirements);
        n.objectives.clear();
        return n;
    }

    public static void load(Planet planet) {
        // ==== 1. 为所有方块创建孤立科技节点 ====
        // 消耗 1 个隐藏研究凭证，只能通过 TechTreeNode.onUnlock → quietUnlock 解锁
        ItemStack[] gateCost = ItemStack.with(EFitems.researchGate, 1);
        orphanNode(transportBelt, gateCost);
        orphanNode(itemControlPort, gateCost);
        orphanNode(splitter, gateCost);
        orphanNode(beltBridge, gateCost);
        orphanNode(converger, gateCost);
        orphanNode(refiningUnit, gateCost);
        orphanNode(mouldingUnit, gateCost);
        orphanNode(shreddingUnit, gateCost);
        orphanNode(fittingUnit, gateCost);
        orphanNode(packagingUnit, gateCost);
        orphanNode(electricPylon, gateCost);
        orphanNode(relayTower, gateCost);
        orphanNode(thermalBank, gateCost);
        orphanNode(protocolStash, gateCost);
        orphanNode(seedPickingUnit, gateCost);
        orphanNode(plantingUnit, gateCost);
        orphanNode(grindingUnit, gateCost);

        // ==== 2. 配置每个科技树节点解锁的内容 ====
        logisticsI.unlockables.add(transportBelt);
        itemAccessPort.unlockables.add(itemControlPort);
        beltSplitting.unlockables.add(splitter);
        beltBridging.unlockables.add(beltBridge);
        beltConverging.unlockables.add(converger);
        refineI.unlockables.add(refiningUnit);
        materialMoulding.unlockables.add(mouldingUnit);
        shreddingI.unlockables.add(shreddingUnit);
        partsFitting.unlockables.add(fittingUnit);
        packagingTech.unlockables.add(packagingUnit);
        electricityI.unlockables.add(electricPylon);
        powerRelay.unlockables.add(relayTower);
        powerI.unlockables.add(thermalBank);
        fieldStash.unlockables.add(protocolStash);
        grinding.unlockables.add(grindingUnit);
        // 培植工艺一次性解锁采种机和种植机
        planting.unlockables.addAll(seedPickingUnit, plantingUnit);

        // ==== 3. 将所有方块注册到星球 ====
        EFblocks.registerToPlanet(planet);

        // ==== 4. 构建显示用科技树 ====
        planet.techTree = nodeRoot("taelos-II", planet, () -> {
            freeNode(automatedIndustryComplex, () -> {
                freeNode(basicAicPlan, () -> {
                    freeNode(basicAicI, () -> {
                        freeNode(basicAicII, () -> {
                            freeNode(basicAicIII, () -> {});
                        });
                    });
                    freeNode(miningI, () -> {
                        freeNode(miningII, () -> {
                            freeNode(miningIII, () -> {});
                        });
                    });
                    freeNode(logisticsI, () -> {
                        freeNode(itemAccessPort, () -> {
                            freeNode(beltSplitting, () -> {
                                freeNode(beltBridging, () -> {
                                    freeNode(beltConverging, () -> {});
                                });
                            });
                        });
                    });
                    freeNode(refineI, () -> {
                        freeNode(materialMoulding, () -> {
                            freeNode(solidFilling, () -> {
                                freeNode(planting, () -> {
                                    freeNode(grinding, () -> {});
                                });
                            });
                        });
                    });
                    freeNode(shreddingI, () -> {
                        freeNode(partsFitting, () -> {
                            freeNode(packagingTech, () -> {});
                        });
                    });
                    freeNode(electricityI, () -> {
                        freeNode(powerRelay, () -> {
                            freeNode(powerI, () -> {
                                freeNode(depotBus, () -> {});
                            });
                        });
                    });
                    freeNode(fieldStash, () -> {});
                });
            });
            freeNode(valleyIv, () -> {
                freeNode(theHub, () -> {
                    freeNode(originiumSciencePark, () -> {
                        freeNode(originLodespring, () -> {
                            freeNode(powerPlateau, () -> {});
                        });
                    });
                    freeNode(valleyPass, () -> {});
                    freeNode(aburreyQuarry, () -> {});
                });
            });
            freeNode(itemsCategory, () -> {});
        });

        // 为根节点设置 planet 引用
        var rootNode = planet.techTree;
        if (rootNode != null) {
            rootNode.planet = planet;
        }

        // 将所有物品注册到该星球
        EFitems.registerToPlanet(planet);
    }
}
