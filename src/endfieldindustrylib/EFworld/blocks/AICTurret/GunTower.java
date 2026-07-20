package endfieldindustrylib.EFworld.blocks.AICTurret;

import endfieldindustrylib.EFcontents.EFitems;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;

public class GunTower extends ItemTurret {
    public GunTower(String name) {
        super(name);

        requirements(Category.turret, ItemStack.with(EFitems.origocrust, 10));

        size = 2;
        range = 240f;
        reload = 120f;
        rotateSpeed = 5f;
        inaccuracy = 2f;
        maxAmmo = 30;
        ammoPerShot = 1;
        shootSound = Sounds.shoot;

        ammo(
            EFitems.origocrust, new BasicBulletType(6f, 5) {{
                width = 9f;
                height = 14f;
                lifetime = 40f;
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
            }},
            EFitems.amethystFiber, new BasicBulletType(6f, 10) {{
                width = 9f;
                height = 14f;
                lifetime = 40f;
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
            }}
        );
    }
}
