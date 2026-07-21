package endfieldindustrylib.EFcontents;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.game.Objectives.*;
import mindustry.type.ItemStack;
import mindustry.type.Planet;

import static endfieldindustrylib.EFcontents.EFblocks.*;
import static endfieldindustrylib.EFcontents.EFitems.*;
import static mindustry.content.TechTree.*;

/**
 * 塔卫二 (taelos-II) 科技树。
 * 注册所有物品和方块到该星球。
 */
public class EFTechTree {

    public static void load(Planet planet) {
        planet.techTree = nodeRoot("taelos-II", transportBelt, () -> {
            // ===================================================================
            // 1. 物流运输
            // ===================================================================
            node(transportBelt, () -> {
                node(itemControlPort, () -> {});
                node(splitter, () -> {});
                node(beltBridge, () -> {});
                node(converger, () -> {});
                node(protocolStash, () -> {});
            });

            // ===================================================================
            // 2. 基础生产
            // ===================================================================
            // ———— 矿物 ————
            nodeProduce(originiumOre, () -> {
                nodeProduce(amethystOre, () -> {});
                nodeProduce(ferriumOre, () -> {});

                // ———— 精炼炉（将矿石加工为材料）————
                node(refiningUnit, () -> {
                    // 精炼炉产出物
                    nodeProduce(ferrium, () -> {});
                    nodeProduce(amethystFiber, () -> {});
                    nodeProduce(origocrust, () -> {});
                    nodeProduce(carbon, () -> {});

                    // ———— 粉碎机（将材料粉碎为粉末）————
                    node(shreddingUnit, () -> {
                        nodeProduce(ferriumPowder, () -> {});
                        nodeProduce(amethystPowder, () -> {});
                        nodeProduce(originiumPowder, () -> {});
                        nodeProduce(carbonPowder, () -> {});
                        nodeProduce(origocrustPowder, () -> {});
                        nodeProduce(buckflowerPowder, () -> {});
                        nodeProduce(citromePowder, () -> {});
                        nodeProduce(sandleafPowder, () -> {});
                        nodeProduce(aketinePowder, () -> {});
                        nodeProduce(jincaoPowder, () -> {});
                        nodeProduce(yazhenPowder, () -> {});

                        // ———— 配件机（将材料加工为零件）————
                        node(fittingUnit, () -> {
                            nodeProduce(ferriumPart, () -> {
                                // ———— 研磨机（通过零件+砂叶粉进一步研磨）————
                                node(grindingUnit, () -> {
                                    nodeProduce(denseFerriumPowder, () -> {});
                                    nodeProduce(crystonPowder, () -> {});
                                    nodeProduce(denseOriginiumPowder, () -> {});
                                    nodeProduce(denseCarbonPowder, () -> {});
                                    nodeProduce(denseOrigocrustPowder, () -> {});
                                    nodeProduce(groundBuckflowerPowder, () -> {});
                                    nodeProduce(groundCitromePowder, () -> {});
                                });
                            });
                            nodeProduce(amethystPart, () -> {
                                // ———— 封装机（零件组装）————
                                node(packagingUnit, () -> {
                                    nodeProduce(industrialExplosive, () -> {});
                                    nodeProduce(lcValleyBattery, () -> {
                                        nodeProduce(scValleyBattery, () -> {
                                            nodeProduce(hcValleyBattery, () -> {
                                                nodeProduce(lcWulingBattery, () -> {});
                                            });
                                        });
                                    });
                                });
                            });
                            nodeProduce(steelPart, () -> {});
                            nodeProduce(crystonPart, () -> {});
                        });

                        // ———— 塑形机（将材料塑形为瓶子）————
                        node(mouldingUnit, () -> {
                            nodeProduce(ferriumBottle, () -> {});
                            nodeProduce(amethystBottle, () -> {});
                            nodeProduce(steelBottle, () -> {});
                            nodeProduce(crystonBottle, () -> {});
                        });
                    });
                });
            });

            // ———— 植物 ————
            nodeProduce(wood, () -> {});
            nodeProduce(buckflower, () -> {
                nodeProduce(firebuckle, () -> {});
                // ———— 采种机 ————
                node(seedPickingUnit, () -> {
                    nodeProduce(buckflowerSeed, () -> {});
                    nodeProduce(citromeSeed, () -> {});
                    nodeProduce(aketineSeed, () -> {});
                    nodeProduce(sandleafSeed, () -> {});
                    nodeProduce(tartpepperSeed, () -> {});
                    nodeProduce(reedRyeSeed, () -> {});
                    nodeProduce(redjadeGinsengSeed, () -> {});
                    nodeProduce(amberRiceSeed, () -> {});

                    // ———— 种植机 ————
                    node(plantingUnit, () -> {
                        nodeProduce(citrome, () -> {
                            nodeProduce(umbraline, () -> {});
                        });
                        nodeProduce(aketine, () -> {});
                        nodeProduce(sandleaf, () -> {});
                    });
                });
            });
            nodeProduce(jincao, () -> {
                nodeProduce(fluffedJincao, () -> {});
            });
            nodeProduce(yazhen, () -> {
                nodeProduce(thornyYazhen, () -> {});
            });
            nodeProduce(tartpepper, () -> {});
            nodeProduce(reedRye, () -> {});
            nodeProduce(redjadeGinseng, () -> {});
            nodeProduce(amberRice, () -> {});

            // ===================================================================
            // 3. 工业产物（精炼炉进阶产物）
            // ===================================================================
            nodeProduce(stabilizedCarbon, () -> {});
            nodeProduce(packedOrigocrust, () -> {});
            nodeProduce(crystonFiber, () -> {});
            nodeProduce(steel, () -> {
                nodeProduce(xiranite, () -> {});
            });
            nodeProduce(amethystFiber, () -> {
                nodeProduce(crystonFiber, () -> {});
            });
            nodeProduce(ferrium, () -> {
                nodeProduce(steel, () -> {});
            });

            // ===================================================================
            // 4. 装备原件（暂未启用）
            // ===================================================================
            // nodeProduce(amethystComponent, () -> {});
            // nodeProduce(ferriumComponent, () -> {});
            // nodeProduce(crystonComponent, () -> {});
            // nodeProduce(xiraniteComponent, () -> {});

            // ===================================================================
            // 5. 电力供应
            // ===================================================================
            node(electricPylon, () -> {
                node(relayTower, () -> {});
                node(thermalBank, () -> {});
            });
        });

        // 为根节点设置 planet 引用，确保 Planet.init() 能关联到此科技树
        planet.techTree.planet = planet;
    }
}
