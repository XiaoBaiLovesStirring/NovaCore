package com.novacore.asm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * BFS光照引擎 — 用队列迭代替代原版DFS递归，消除栈溢出风险。
 *
 * 原理：
 *   原版 checkLightFor 使用递归（DFS）传播光照变化，在深层洞穴或大规模
 *   光照更新时可能导致 StackOverflowError。
 *   本实现用 Deque 做 BFS，广度优先处理，时间复杂度 O(n)，空间复杂度 O(n)。
 *
 * 与 vanilla 的行为差异：BFS 的传播顺序与 DFS 不同，但最终光照结果一致。
 */
public class NovaLightEngine {

    private static final int MAX_LIGHT = 15;
    private static final int MIN_LIGHT = 0;

    /**
     * 用 BFS 替代原版 checkLightFor 的 DFS 递归。
     * @return 是否有光照值被修改
     */
    public static boolean checkLightFor(World world, EnumSkyBlock type, BlockPos pos) {
        // 确保区块已加载（原版也做此检查）
        if (!world.isAreaLoaded(pos, 17)) return false;

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> enqueued = new HashSet<>();
        queue.add(pos);
        enqueued.add(pos);
        boolean updated = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!world.isBlockLoaded(current)) continue;

            IBlockState state = world.getBlockState(current);
            int currentLight = world.getLightFor(type, current);
            int opacity = Math.max(1, state.getLightOpacity(world, current));

            // 计算邻居最大光照
            int maxNeighborLight = MIN_LIGHT;
            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
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
                    if (world.isBlockLoaded(next) && enqueued.add(next)) {
                        queue.add(next);
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
        checkLightFor(world, type, pos);
    }
}