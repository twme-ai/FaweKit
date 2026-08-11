package dev.twme.fawekit.service;

import com.sk89q.worldedit.regions.Region;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionStackService {
    private final Map<UUID, History> histories = new ConcurrentHashMap<>();

    public History history(UUID playerId) {
        return histories.computeIfAbsent(playerId, ignored -> new History());
    }

    public static final class History {
        private List<Region> stack = new ArrayList<>();
        private final Deque<List<Region>> undo = new ArrayDeque<>();
        private final Deque<List<Region>> redo = new ArrayDeque<>();

        public List<Region> stack() {
            return List.copyOf(stack);
        }

        public Region get(int oneBasedIndex) {
            int index = normalize(oneBasedIndex, stack.size());
            return stack.get(index).clone();
        }

        public void push(Region region, int oneBasedIndex) {
            checkpoint();
            int index = oneBasedIndex == 0 ? 0 : insertionIndex(oneBasedIndex, stack.size());
            stack.add(index, region.clone());
        }

        public List<Region> pop(int count) {
            if (count < 1 || count > stack.size()) {
                throw new IllegalArgumentException("Pop count must be between 1 and " + stack.size() + '.');
            }
            checkpoint();
            List<Region> result = new ArrayList<>(stack.subList(0, count));
            stack.subList(0, count).clear();
            return result;
        }

        public void delete(int oneBasedIndex) {
            checkpoint();
            stack.remove(normalize(oneBasedIndex, stack.size()));
        }

        public void clear() {
            checkpoint();
            stack.clear();
        }

        public void replace(List<Region> regions) {
            checkpoint();
            stack = copy(regions);
        }

        public boolean undo() {
            if (undo.isEmpty()) {
                return false;
            }
            redo.push(copy(stack));
            stack = undo.pop();
            return true;
        }

        public boolean redo() {
            if (redo.isEmpty()) {
                return false;
            }
            undo.push(copy(stack));
            stack = redo.pop();
            return true;
        }

        private void checkpoint() {
            undo.push(copy(stack));
            while (undo.size() > 50) {
                undo.removeLast();
            }
            redo.clear();
        }

        private static List<Region> copy(List<Region> regions) {
            return new ArrayList<>(regions.stream().map(Region::clone).toList());
        }

        private static int normalize(int oneBasedIndex, int size) {
            if (size == 0) {
                throw new IllegalArgumentException("The selection stack is empty.");
            }
            int index = oneBasedIndex > 0 ? oneBasedIndex - 1 : size + oneBasedIndex;
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException("Selection index is outside the stack.");
            }
            return index;
        }

        private static int insertionIndex(int oneBasedIndex, int size) {
            if (oneBasedIndex > 0) {
                return Math.min(oneBasedIndex - 1, size);
            }
            return Math.max(0, size + oneBasedIndex + 1);
        }
    }
}
