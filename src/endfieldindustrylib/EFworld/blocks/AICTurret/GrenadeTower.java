package endfieldindustrylib.EFworld.blocks.AICTurret;

import arc.graphics.Color;
import endfieldindustrylib.EFcontents.EFitems;
import mindustry.content.Items;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;

public class GrenadeTower extends ItemTurret {
    public GrenadeTower(String name) {
        super(name);

        requirements(Category.turret, ItemStack.with(EFitems.origocrust, 15));

        consumePower(5f);

        size = 2;
        range = 320f;
        reload = 180f;
        rotateSpeed = 4f;
        inaccuracy = 3f;
        maxAmmo = 20;
        ammoPerShot = 1;
        shootSound = Sounds.shootArtillery;

        targetAir = false;
        targetGround = true;

        ammo(
            EFitems.aketinePowder, new ArtilleryBulletType(4f, 0) {{
                splashDamage = 800f;
                splashDamageRadius = 40f;
                trailColor = Color.valueOf("8ae86a");
                hitColor = Color.valueOf("8ae86a");
                backColor = Color.valueOf("6bbd50");
                frontColor = Color.valueOf("a0f080");

                width = 8f;
                height = 10f;
                lifetime = 80f;
            }},
            Items.blastCompound, new ArtilleryBulletType(4f, 0) {{
                splashDamage = 1200f;
                splashDamageRadius = 60f;
                trailColor = Color.valueOf("ff795e");
                hitColor = Color.valueOf("ff795e");
                backColor = Color.valueOf("e06040");
                frontColor = Color.valueOf("ffa080");

                width = 10f;
                height = 12f;
                lifetime = 80f;
            }}
        );
    }
}
