package org.cloudbus.cloudsim.examples;

import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.autoscaling.VerticalVmScalingSimple;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.core.Simulation;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.resources.Processor;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingDouble;

public class VerticalScaling {
	private static final int SCHEDULING_INTERVAL = 5;
	private static final int HOSTS = 1;
	private static final int HOST_PES = 8;
	private static final int VMS = 2;
	private static final int VM_PES = 2;
	private static final int VM_RAM = 1000;
	private static final int VM_MIPS = 1000;
	private static final int VM_BW = 1000;
	private static final int VM_SIZE = 10000;
	private static final int CLOUDLETS = 20;

	private final CloudSimPlus simulation;
	private final DatacenterBroker broker0;
	private List<Host> hostList;
	private List<Vm> vmList;
	private List<Cloudlet> cloudletList;
	private int createsVms;

	public static void main(String[] args) {
		new VerticalScaling();
	}

	private VerticalScaling() {
		hostList = new ArrayList<>(HOSTS);
		vmList = new ArrayList<>(VMS);
		cloudletList = new ArrayList<>(CLOUDLETS);

		simulation = new CloudSimPlus();
		simulation.addOnClockTickListener(this::onClockTickListener);

		createDatacenter();
		broker0 = new DatacenterBrokerSimple(simulation);

		vmList.addAll(createListOfScalableVms(VMS));

		createCloudletListsWithDifferentDelays();
		broker0.submitVmList(vmList);
		broker0.submitCloudletList(cloudletList);

		simulation.start();

		printSimulationResults();
	}

	private void onClockTickListener(EventInfo evt) {
		vmList.forEach(vm -> System.out.printf(
			"\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). RAM usage: %.2f%% (%d MB)%n",
			evt.getTime(), vm.getId(), vm.getCpuPercentUtilization() * 100.0, vm.getPesNumber(),
			vm.getCloudletScheduler().getCloudletExecList().size(),
			vm.getRam().getPercentUtilization() * 100, vm.getRam().getAllocatedResource())
		);
	}

	private void printSimulationResults() {
		final List<Cloudlet> finishedCloudletList = broker0.getCloudletFinishedList();
		final Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
		final Comparator<Cloudlet> sortByStartTime = comparingDouble(Cloudlet::getStartTime);
		finishedCloudletList.sort(sortByVmId.thenComparing(sortByStartTime));

		new CloudletsTableBuilder(finishedCloudletList).build();
	}

	private void createDatacenter() {
		for (int i = 0; i < HOSTS; i++) {
			hostList.add(createHost());
		}
		final DatacenterSimple dc0 = new DatacenterSimple(simulation, hostList);
		dc0.setSchedulingInterval(SCHEDULING_INTERVAL);
	}

	private Host createHost() {
		final ArrayList<Pe> peList = new ArrayList<Pe>(HOST_PES);
		for (int i = 0; i < HOST_PES; i++) {
			peList.add(new PeSimple(1000));
		}
		final long ram = 20000;
		final long bw = 20000;
		final long storage = 10000000;
		return new HostSimple(ram, bw, storage, peList).setVmScheduler(new VmSchedulerTimeShared());
	}

	private List<Vm> createListOfScalableVms(final int vmsNumber) {
		final ArrayList<Vm> newVmList = new ArrayList<Vm>(vmsNumber);
		for (int i = 0; i < vmsNumber; i++) {
			final Vm vm = createVm();
			vm.setPeVerticalScaling(createVerticalPeScaling());
			newVmList.add(vm);
		}
		return newVmList;
	}

	private Vm createVm() {
		final int id = createsVms++;
		return new VmSimple(id, VM_MIPS, VM_PES)
			.setRam(VM_RAM)
			.setBw(VM_BW)
			.setSize(VM_SIZE);
	}

	private VerticalVmScaling createVerticalPeScaling() {
		final double scalingFactor = 0.1;
		final VerticalVmScalingSimple verticalCpuScaling = new VerticalVmScalingSimple(Processor.class, scalingFactor);
		verticalCpuScaling.setResourceScaling(vs -> vs.getScalingFactor() * vs.getAllocatedResource())
			//.setLowerThresholdFunction(this::lowerCpuUtilizationThreshold)
			.setUpperThresholdFunction(this::upperCpuUtilizationThreshold);
		return verticalCpuScaling;
	}

	private double lowerCpuUtilizationThreshold(Vm vm) {
		return 0.4;
	}

	private double upperCpuUtilizationThreshold(Vm vm) {
		return 0.6;  // Updated to 60% as per the criteria
	}

	private void createCloudletListsWithDifferentDelays() {
		final int pesNumber = 2;
		for (int i = 1; i <= CLOUDLETS; i++) {
			final int delay = i * SCHEDULING_INTERVAL;
			final int length = 10000 + (i - 1) * 10000;
			cloudletList.add(createCloudlet(length, pesNumber, delay));
		}
	}

	private Cloudlet createCloudlet(final long length, final int pesNumber, final double delay) {
		final UtilizationModelFull utilizationCpu = new UtilizationModelFull();
		final UtilizationModelDynamic utilizationModelDynamic = new UtilizationModelDynamic(0.01);
		final CloudletSimple cl = new CloudletSimple(length, pesNumber);
		cl.setUtilizationModelBw(utilizationModelDynamic)
			.setUtilizationModelRam(utilizationModelDynamic)
			.setUtilizationModelCpu(utilizationCpu)
			.setSubmissionDelay(delay);
		return cl;
	}
}
