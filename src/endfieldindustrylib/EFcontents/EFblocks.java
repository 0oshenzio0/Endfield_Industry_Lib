package endfieldindustrylib.EFcontents;

import endfieldindustrylib.EFworld.blocks.AICBasicFacility.FittingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.GrindingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.MouldingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.PackagingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.PlantingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.RectGenericAICBasicFacility;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.RefiningUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.SeedPickingUnit;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.ShreddingUnit;
import endfieldindustrylib.EFworld.blocks.AICDepotAccess.ProtocolStash;
import endfieldindustrylib.EFworld.blocks.AICPower.ElectricPylon;
import endfieldindustrylib.EFworld.blocks.AICPower.RelayTower;
import endfieldindustrylib.EFworld.blocks.AICPower.ThermalBank;
import endfieldindustrylib.EFworld.blocks.AICTransport.BeltBridge;
import endfieldindustrylib.EFworld.blocks.AICTransport.Converger;
import endfieldindustrylib.EFworld.blocks.AICTransport.ItemControlPort;
import endfieldindustrylib.EFworld.blocks.AICTransport.Splitter;
import endfieldindustrylib.EFworld.blocks.AICTransport.TransportBelt;

public class EFblocks {
    // ===== 物流运输 =====
    public static TransportBelt transportBelt;
    public static ItemControlPort itemControlPort;
    public static Splitter splitter;
    public static BeltBridge beltBridge;
    public static Converger converger;
    public static ProtocolStash protocolStash;
    // ===== 基础生产 =====
    public static SeedPickingUnit seedPickingUnit;
    public static PlantingUnit plantingUnit;
    public static RefiningUnit refiningUnit;
    public static ShreddingUnit shreddingUnit;
    public static FittingUnit fittingUnit;
    public static MouldingUnit mouldingUnit;
    // ===== 合成制造 =====
    public static PackagingUnit packagingUnit;
    public static GrindingUnit grindingUnit;
    //public static GearingUnit gearingUnit;        //装备原件机
    //public static FillingUnit fillingUnit;        //灌装机
    //public static SeparatingUnit separatingUnit;        //拆解机
    //public static ReactorCrucible reactorCrucible;        //反应池
    //public static ForgeoftheSky forgeoftheSky;        //天有洪炉
    // ===== 电力供应 =====
    public static RelayTower relayTower;
    public static ElectricPylon electricPylon;
    public static ThermalBank thermalBank;
    //public static XiraniteRelay xiraniteRelay;        //息壤中继器
    //public static XiranitePylon xiranitePylon;        //息壤供电桩

    public static void load() {
        // 注册矩形多块工厂所需的子方块
        RectGenericAICBasicFacility.registerChildBlock();
        // 物流运输
        transportBelt = new TransportBelt("transport-belt");              // 传送带
        transportBelt.load();
        itemControlPort = new ItemControlPort("item-control-port");       // 物品准入口
        itemControlPort.load();
        splitter = new Splitter("splitter");                              // 分流器
        splitter.load();
        beltBridge = new BeltBridge("belt-bridge");                       // 物流桥
        beltBridge.load();
        converger = new Converger("converger");                           // 汇流器
        converger.load();
        protocolStash = new ProtocolStash("protocol-stash");              // 协议储存箱
        protocolStash.load();
        // 基础生产
        seedPickingUnit = new SeedPickingUnit("seed-picking-unit");       // 采种机
        seedPickingUnit.load();
        plantingUnit = new PlantingUnit("planting-unit");                 // 种植机
        plantingUnit.load();
        refiningUnit = new RefiningUnit("refining-unit");                 // 精炼炉
        refiningUnit.load();
        shreddingUnit = new ShreddingUnit("shredding-unit");              // 粉碎机
        shreddingUnit.load();
        fittingUnit = new FittingUnit("fitting-unit");                    // 配件机
        fittingUnit.load();
        mouldingUnit = new MouldingUnit("moulding-unit");                 // 塑形机
        mouldingUnit.load();
        // 合成制造
        //gearingUnit = new GearingUnit("gearing-unit", 4, 6);        //装备原件机
        //gearingUnit.load();
        //fillingUnit = new FillingUnit("filling-unit", 4, 6);        //灌装机
        //fillingUnit.load();
        packagingUnit = new PackagingUnit("packaging-unit", 4, 6);        // 封装机
        packagingUnit.load();
        grindingUnit = new GrindingUnit("grinding-unit", 4, 6);           // 研磨机
        grindingUnit.load();
        //separatingUnit = new SeparatingUnit("separating-unit", 4, 6);        //拆解机
        //separatingUnit.load();
        //reactorCrucible = new ReactorCrucible("reactor-crucible", 4, 6);        //反应池
        //reactorCrucible.load();
        //forgeoftheSky = new ForgeoftheSky("forge-of-the-sky");        //天有洪炉
        //forgeoftheSky.load();
        // 电力供应
        relayTower = new RelayTower("relay-tower");                       // 中继器
        relayTower.load();
        electricPylon = new ElectricPylon("electric-pylon");              // 供电桩
        electricPylon.load();
        thermalBank = new ThermalBank("thermal-bank");                    // 热能池
        thermalBank.load();
        //xiraniteRelay = new XiraniteRelay("xiranite-relay");        //息壤中继器
        //xiraniteRelay.load();
        //xiranitePylon = new XiranitePylon("xiranite-pylon");        //息壤供电桩
        //xiranitePylon.load();
    }
}