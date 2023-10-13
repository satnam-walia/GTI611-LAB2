package org.cloudbus.cloudsim.examples;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class PremierExemple_Aut23 {
	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;
	private static final int NbCloudlets = 12;  // Updated number of cloudlets to 12

	public static void main(String[] args) {
		Log.printLine("Starting PremierExemple_Aut23..");
		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
			CloudSim.init(num_user, calendar, trace_flag);

			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			DatacenterBroker broker = createBroker(1);
			int brokerId = broker.getId();

			vmlist = createVM(brokerId, 5, 1); // Updated number of VMs to 5

			broker.submitVmList(vmlist);
			cloudletList = createCloudlet(brokerId, NbCloudlets, 1);
			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();
			List<Cloudlet> newList1 = broker.getCloudletReceivedList();
			CloudSim.stopSimulation();

			Log.print("=============> User " + brokerId + "    ");
			printCloudletList(newList1);
			Log.printLine("Simulation finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList = new ArrayList<Pe>();
		int mips = 1000;
		for (int i = 0; i < 4; i++) {  // Updated to 4 CPU cores
			peList.add(new Pe(i, new PeProvisionerSimple(mips)));
		}
		int ram = 2048;
		long storage = 1000000;
		int bw = 10000;
		for (int hostId = 1; hostId <= 2; hostId++) {  // Updated to 2 hosts
			hostList.add(
				new Host(
					hostId,
					new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bw),
					storage,
					new ArrayList<>(peList),  // Create a new list for each host
					new VmSchedulerTimeShared(peList)
				)
			);
		}
		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;

		LinkedList<Storage> storageList = new LinkedList<Storage>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
			arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return datacenter;
	}

	private static DatacenterBroker createBroker(int id) {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker" + id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	private static List<Vm> createVM(int userId, int vms, int idShift) {
		LinkedList<Vm> list = new LinkedList<Vm>();
		long size = 10000;
		int ram = 1024;
		int mips = 500;  // Updated mips to 500
		long bw = 1000;
		int pesNumber = 2;  // VMs with 2 CPU cores
		String vmm = "Xen";
		Vm[] vm = new Vm[vms];
		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
			list.add(vm[i]);
		}
		return list;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
		long length = 3000;  // Updated length to 3000
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		Cloudlet[] cloudlet = new Cloudlet[cloudlets];
		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}
		return list;
	}

	private static void printCloudletList(List<Cloudlet> list) throws IOException {
		int size = list.size();
		Cloudlet cloudlet;
		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");
		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");
				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}
	}
}
