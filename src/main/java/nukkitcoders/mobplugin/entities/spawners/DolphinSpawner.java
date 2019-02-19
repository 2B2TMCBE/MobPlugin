package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWater;
import cn.nukkit.entity.passive.EntityDolphin;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.autospawn.SpawnResult;


public class DolphinSpawner extends AbstractEntitySpawner {

    public DolphinSpawner(AutoSpawnTask spawnTask, Config pluginConfig) {
        super(spawnTask, pluginConfig);
    }

    public SpawnResult spawn(Player player, Position pos, Level level) {
        SpawnResult result = SpawnResult.OK;

        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        final int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);

        if (blockId != Block.WATER && blockId != Block.STILL_WATER) {
            result = SpawnResult.WRONG_BLOCK;
        } else if (biomeId != 0) {
            result = SpawnResult.WRONG_BIOME;
        } else if (pos.y > 127 || pos.y < 1 || blockId == Block.AIR) {
            result = SpawnResult.POSITION_MISMATCH;
        } else if (level.getName().equals("nether") || level.getName().equals("end")) {
            result = SpawnResult.WRONG_BIOME;
        } else {
            if (level.getBlock(pos.add(0, -1, 0)) instanceof BlockWater) {
                this.spawnTask.createEntity("Dolphin", pos.add(0, -1, 0));
            } else {
                result = SpawnResult.POSITION_MISMATCH;
            }
        }

        return result;
    }

    @Override
    public final int getEntityNetworkId() {
        return EntityDolphin.NETWORK_ID;
    }
}
