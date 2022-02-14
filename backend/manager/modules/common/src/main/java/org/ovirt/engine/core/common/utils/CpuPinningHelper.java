package org.ovirt.engine.core.common.utils;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VdsCpuUnit;

public class CpuPinningHelper {

    private static Collection<Integer> parsePCpuPinningNumbers(final String text) {
        try {
            Set<Integer> include = new HashSet<>();
            Set<Integer> exclude = new HashSet<>();
            String[] splitText = text.split(",");
            for (String section : splitText) {
                if (section.startsWith("^")) {
                    exclude.add(Integer.parseInt(section.substring(1)));
                } else if (section.contains("-")) {
                    // include range
                    String[] numbers = section.split("-");
                    int start = Integer.parseInt(numbers[0]);
                    int end = Integer.parseInt(numbers[1]);
                    List<Integer> range = createRange(start, end);
                    if (range != null) {
                        include.addAll(range);
                    } else {
                        return Arrays.asList();
                    }
                } else {
                    // include one
                    include.add(Integer.parseInt(section));
                }
            }
            include.removeAll(exclude);
            return include;
        } catch (NumberFormatException ex) {
            return Arrays.asList();
        }
    }

    /**
     * Find out which host cpus are pinned to virtual cpus
     * @param cpuPinning cpu pinning rules string
     * @return a set of all used host cpus
     */
    public static Set<Integer> getAllPinnedPCpus(String cpuPinning) {
        // collect all pinned cpus and merge them into one set
        final Set<Integer> pinnedCpus = new LinkedHashSet<>();
        for (final String rule : cpuPinning.split("_")) {
            pinnedCpus.addAll(CpuPinningHelper.parsePCpuPinningNumbers(rule.split("#")[1]));
        }
        return pinnedCpus;
    }

    /**
     * Parse the provided cpu pinning rules and return the parsed rules
     * @param cpuPinning cpu pinning rules string
     * @return a list containing virtual cpu to host cpu associations
     */
    public static List<PinnedCpu> parseCpuPinning(final String cpuPinning) {
        if (cpuPinning == null || cpuPinning.isEmpty()) {
            return Collections.emptyList();
        }

        final List<PinnedCpu> rules = new ArrayList<>();
        for (final String rule : cpuPinning.split("_")) {
            String[] splitRule = rule.split("#");
            Collection<Integer> pCpus = parsePCpuPinningNumbers(splitRule[1]);
            rules.add(new PinnedCpu(Integer.parseInt(splitRule[0]), pCpus));
        }
        return rules;
    }

    private static List<Integer> createRange(int start, int end) {
        if (start >= 0 && start < end) {
            List<Integer> returnList = new LinkedList<>();
            for (int i = start; i <= end; i++) {
                returnList.add(i);
            }
            return returnList;
        } else {
            return null;
        }
    }

    public static String getVmPinning(VM vm) {
        switch (vm.getCpuPinningPolicy()) {
            case MANUAL:
                return vm.getCpuPinning();
            case RESIZE_AND_PIN_NUMA:
            case DEDICATED:
                return vm.getCurrentCpuPinning();
            default:
                return null;
        }
    }

    /**
     * Provides a CPU pinning string based on the VdsCpuUnit list.
     * The pinning will be based on the list order.
     *
     * @param vdsCpuUnits a list of VdsCpuUnit
     * @return a string of CPU pinning
     */
    public static String createCpuPinning(List<VdsCpuUnit> vdsCpuUnits) {
        return IntStream.range(0, vdsCpuUnits.size())
                .mapToObj(i-> String.format("%d#%d", i, vdsCpuUnits.get(i).getCpu())).collect(Collectors.joining("_"));
    }

    /**
     * Represents the association between a virtual CPU in a VM to the bare metal cpu threads on a host
     */
    public static class PinnedCpu {

        private Integer vCpu;
        private Collection<Integer> pCpus;

        protected PinnedCpu() {

        }

        protected PinnedCpu(Integer vCpu, Collection<Integer> pCpus) {
            this.vCpu = requireNonNull(vCpu);
            this.pCpus = requireNonNull(pCpus);
        }

        /**
         * Get the virtual cpu which is associated with the host cpu threads in this class
         * @return the virtual cpu
         */
        public Integer getvCpu() {
            return vCpu;
        }

        /**
         * Get host cpu threads which are associated with the virtual cpu in this class
         * @return list of associated host cpu thread
         */
        public Collection<Integer> getpCpus() {
            return pCpus;
        }
    }
}
