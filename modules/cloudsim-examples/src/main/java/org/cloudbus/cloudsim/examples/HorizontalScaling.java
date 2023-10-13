package org.cloudbus.cloudsim.examples;


import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.HorizontalVmScalingSimple;
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
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;


public class HorizontalScaling {

	private static final int SCHEDULING_INTERVAL = 5;
	private static final int CLOUDLETS_CREATION_INTERVAL = SCHEDULING_INTERVAL;

	private static final int HOSTS = 1;
	private static final int HOST_PES = 8;
	private static final int VMS = 2;
	private static final int CLOUDLETS = 20;
	private static final long[] CLOUDLET_LENGTHS =
		{10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000,
			110000, 120000, 130000, 140000, 150000, 160000, 170000, 180000, 190000, 200000};

	private final CloudSimPlus simulation;
	private final Datacenter dc0;
	private final DatacenterBroker broker0;
	private final List<Host> hostList = new ArrayList<>(HOSTS);
	private final List<Vm> vmList = new ArrayList<>(VMS);
	private final List<Cloudlet> cloudletList = new ArrayList<>(CLOUDLETS);
	private int createdCloudlets;
	private int createsVms;

	public static void main(String[] args) {
		new HorizontalScaling();
	}

	private HorizontalScaling() {
		simulation = new CloudSimPlus();
		dc0 = createDatacenter();
		broker0 = new DatacenterBrokerSimple(simulation);
		//broker0.setVmDestructionDelay(10.0);
		vmList.addAll(createListOfScalableVms(VMS));
		createCloudletList();
		broker0.submitVmList(vmList);
		broker0.submitCloudletList(cloudletList);
		simulation.start();
		new CloudletsTableBuilder(broker0.getCloudletFinishedList()).build();
	}

	private Datacenter createDatacenter() {
		hostList.add(createHost());
		return new DatacenterSimple(simulation, hostList).setSchedulingInterval(SCHEDULING_INTERVAL);
	}

	private Host createHost() {
		final ArrayList<Pe> peList = new ArrayList<Pe>(HOST_PES);
		for (int i = 0; i < HOST_PES; i++) {
			peList.add(new PeSimple(1000));
		}
		return new HostSimple(20000, 20000, 10000000, peList)
			.setRamProvisioner(new ResourceProvisionerSimple())
			.setBwProvisioner(new ResourceProvisionerSimple())
			.setVmScheduler(new VmSchedulerTimeShared());
	}

	private List<Vm> createListOfScalableVms(final int vmsNumber) {
		final ArrayList<Vm> newVmList = new ArrayList<Vm>(vmsNumber);
		for (int i = 0; i < vmsNumber; i++) {
			final Vm vm = createVm();
			createHorizontalVmScaling(vm);
			newVmList.add(vm);
		}
		return newVmList;
	}

	private void createHorizontalVmScaling(final Vm vm) {
		final HorizontalVmScalingSimple horizontalScaling = new HorizontalVmScalingSimple();
		horizontalScaling.setVmSupplier(this::createVm).setOverloadPredicate(this::isVmOverloaded);
		vm.setHorizontalScaling(horizontalScaling);
	}

	private boolean isVmOverloaded(final Vm vm) {
		return vm.getCpuPercentUtilization() > 0.6;
	}

	private Vm createVm() {
		final int id = createsVms++;
		return new VmSimple(id, 1000, 2)
			.setRam(1000).setBw(1000).setSize(10000)
			.setCloudletScheduler(new CloudletSchedulerTimeShared());
	}

	private void createCloudletList() {
		for (int i = 0; i < CLOUDLETS; i++) {
			cloudletList.add(createCloudlet());
		}
	}

	private Cloudlet createCloudlet() {
		final int id = createdCloudlets++;
		final UtilizationModelDynamic utilizadionModelDynamic = new UtilizationModelDynamic(0.1);
		final long length = CLOUDLET_LENGTHS[id % CLOUDLET_LENGTHS.length];
		return new CloudletSimple(id, length, 2)
			.setFileSize(1024)
			.setOutputSize(1024)
			.setUtilizationModelBw(utilizadionModelDynamic)
			.setUtilizationModelRam(utilizadionModelDynamic)
			.setUtilizationModelCpu(new UtilizationModelFull());
	}
}
