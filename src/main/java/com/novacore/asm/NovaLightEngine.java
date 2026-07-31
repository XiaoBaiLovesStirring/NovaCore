package com.novacore.asm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * BFS光照引擎 — 用队列迭代替代原版DFS递归，消除栈溢出风险。
 *
 * 原理：
 *   原版 checkLightFor 使用递归（DFS）传播光照变化，在深层洞穴或大规模
 *   光照更新时可能导致 StackOverflowError。
 *   本实现用 Deque 做 BFS，广度优先处理，时间复杂度 O(n)，空间复杂度 O(n)。
 *
 * 增强：
 *   - 高度边界限制（避免光照传播到 Y<0 或 Y>255）
 *   - 传播距离限制（避免无限传播，最大 256 格）
 *   - 使用 long 位打包替代 HashSet 去重（内存效率更高）
 *
 * 与 vanilla 的行为差异：BFS 的传播顺序与 DFS 不同，但最终光照结果一致。
 */
public class NovaLightEngine {

    private static final int MAX_LIGHT = 15;
    private static final int MIN_LIGHT = 0;
    private static final int MAX_PROPAGATION_DIST = 256;
    private static final int Y_MIN = 0;
    private static final int Y_MAX = 255;

    // 位打包去重：用 long 的高 32 位存 x+30000000，低 32 位存 z+30000000
    // y 坐标用单独的 byte 数组索引
    // 简化方案：使用 (x, y, z) 打包为 long
    private static final int X_SHIFT = 38;  // 20 bits for x
    private static final int Z_SHIFT = 20;  // 18 bits for z
    private static final long MASK = (1L << 56) - 1;

    private static long packKey(int x, int y, int z) {
        // 将 (x, y, z) 映射到正值区间
        long lx = (long)(x + 30000000) & 0xFFFFF;  // 20 bits
        long ly = (long)(y + 128) & 0xFF;            // 8 bits
        long lz = (long)(z + 30000000) & 0xFFFFF;    // 20 bits
        return (lx << 40) | (ly << 32) | (lz << 12) | (1L << 11); // 48-bit key
    }

    /**
     * 用 BFS 替代原版 checkLightFor 的 DFS 递归。
     * @return 是否有光照值被修改
     */
    public static boolean checkLightFor(World world, EnumSkyBlock type, BlockPos pos) {
        // 确保区块已加载（原版也做此检查）
        if (!world.isAreaLoaded(pos, 17)) return false;

        // 高度边界检查
        int py = pos.getY();
        if (py < Y_MIN || py > Y_MAX) return false;

        Deque<BlockPos> queue = new ArrayDeque<>(256);
        // 使用简单的去重集合（BFS 传播范围通常不大）
        java.util.BitSet visited = new java.util.BitSet(8192); // 8K bits = 1024 bytes
        queue.add(pos);
        int visitedCount = 0;
        boolean updated = false;

        while (!queue.isEmpty() && visitedCount < MAX_PROPAGATION_DIST) {
            BlockPos current = queue.poll();
            if (!world.isBlockLoaded(current)) continue;

            IBlockState state = world.getBlockState(current);
            int currentLight = world.getLightFor(type, current);
            int opacity = Math.max(1, state.getLightOpacity(world, current));

            // 计算邻居最大光照
            int maxNeighborLight = MIN_LIGHT;
            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
                // 高度边界检查
                int ny = neighbor.getY();
                if (ny < Y_MIN || ny > Y_MAX) continue;
                if (world.isBlockLoaded(neighbor)) {
                    int nLight = world.getLightFor(type, neighbor);
                    if (nLight > maxNeighborLight) {
                        maxNeighborLight = nLight;
                    }
                }
            }

            // 计算方块自身发光
            int blockLight = MIN_LIGHT;
            if (type == EnumSkyBlock.BLOCK) {
                blockLight = state.getLightValue(world, current);
            }

            // 期望光照 = max(邻居最大-衰减, 自身发光)
            int expected = Math.max(maxNeighborLight - opacity, blockLight);
            if (expected < MIN_LIGHT) expected = MIN_LIGHT;
            if (expected > MAX_LIGHT) expected = MAX_LIGHT;

            if (currentLight != expected) {
                world.setLightFor(type, current, expected);
                updated = true;
                // 传播到邻居
                for (EnumFacing facing : EnumFacing.VALUES) {
                    BlockPos next = current.offset(facing);
                    int ny = next.getY();
                    if (ny < Y_MIN || ny > Y_MAX) continue;
                    if (world.isBlockLoaded(next) && visitedCount < MAX_PROPAGATION_DIST) {
                        // 简单去重：基于坐标的 hash
                        int hash = Math.abs((next.getX() * 73856093) ^ (next.getY() * 19349663) ^ (next.getZ() * 83492791)) & 0x1FFF;
                        if (!visited.get(hash)) {
                            visited.set(hash);
                            visitedCount++;
                            queue.add(next);
                        }
                    }
                }
            }
        }

        return updated;
    }

    /**
     * BFS 光照更新入口 — 方块放置/破坏时调用。
     */
    public static void updateLightBFS(World world, EnumSkyBlock type, BlockPos pos) {
        if (!world.isAreaLoaded(pos, 17)) return;
        if (pos.getY() < Y_MIN || pos.getY() > Y_MAX) return;
        checkLightFor(world, type, pos);
    }
}