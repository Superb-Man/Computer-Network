/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 TerrorMOdelple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/csma-module.h"
#include "ns3/internet-module.h"
#include "ns3/mobility-module.h"
#include "ns3/network-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/propagation-loss-model.h"
#include "ns3/ssid.h"
#include "ns3/yans-wifi-helper.h"

// Default Network Topology
//
//   Wifi 10.1.3.0
//                 AP
//  *    *    *    *
//  |    |    |    |    10.1.1.0
// n5   n6   n7   n0 -------------- n1   n2   n3   n4
//                   point-to-point  |    |    |    |
//                                   ================
//                                     LAN 10.1.2.0

NS_LOG_COMPONENT_DEFINE("staticWirelessDumbbell");

uint64_t tot_bytes_rcvd = 0;
uint64_t tot_bytes_sent = 0;


#define NODE 0
#define FLOW 1
#define PCKTSPERSEC 2
#define COVERAGEAREA 3
#define NODESPEED 4


struct Flow_helper : public ns3::Object
{
    uint64_t tot_bits_rcvd = 0 ;
    uint32_t tot_pkt_rcvd = 0 ;
    uint32_t tot_pkt_sent = 0 ;
    uint32_t pay_load_size = 1<<10 ;

    Flow_helper() {
        tot_bits_rcvd = tot_pkt_rcvd = tot_pkt_sent = 0 ;
        pay_load_size = 1 << 10 ;
    }

    static ns3::TypeId GetTypeId()
    {
        static ns3::TypeId tid = ns3::TypeId("MyObject")
                                .SetParent<Object>()
                                .SetGroupName("Flow")
                                .AddConstructor<Flow_helper>();

        return tid;
    }

    ~Flow_helper() {

    }
};

void
pkt_rcvd(ns3::Ptr<Flow_helper>fl, 
    ns3::Ptr<const ns3::Packet>pkt, 
    const ns3::Address& address) 
{
    ns3::Time now = ns3::Simulator::Now();
    fl->tot_pkt_rcvd += pkt->GetSize() / fl->pay_load_size ;
    fl->tot_bits_rcvd += 8 * pkt->GetSize();
    tot_bytes_rcvd += pkt->GetSize();
}

void
pkt_sent(ns3::Ptr<Flow_helper>fl, 
            ns3::Ptr<const ns3::Packet>pkt)
{
    ns3::Time now = ns3::Simulator::Now() ;
    fl->tot_pkt_sent+=1;
    tot_bytes_sent += pkt->GetSize();
}

std::vector<ns3::Ptr<Flow_helper>>
static_solve(uint32_t nodes,uint32_t flows,uint32_t packetsPerSec, uint32_t covArea, uint32_t pay_load_size,bool mobile , uint16_t speed) {
    ns3::Config::SetDefault("ns3::TcpSocket::SegmentSize", ns3::UintegerValue(pay_load_size));
    ns3::Config::SetDefault("ns3::TcpL4Protocol::SocketType", ns3::StringValue("ns3::TcpNewReno"));
    ns3::Config::SetDefault("ns3::TcpSocket::InitialCwnd", ns3::UintegerValue(1));
    ns3::Config::SetDefault("ns3::TcpL4Protocol::RecoveryType",
                       ns3::TypeIdValue(ns3::TypeId::LookupByName("ns3::TcpClassicRecovery")));

    uint64_t x = pay_load_size * 8 * (packetsPerSec + 0.1) ;
    std::string dataRate = std::to_string(x); /* Application layer datarate. */

    std::string coverageArea = std::to_string(covArea);

    uint32_t receivers = nodes ;
    receivers/=2 ;

    uint32_t senders = nodes - receivers;

    ns3::NodeContainer p2pNodes;
    p2pNodes.Create(2);

    ns3::Ptr<ns3::RateErrorModel> errorMOdel = ns3::CreateObject<ns3::RateErrorModel>();
    errorMOdel->SetAttribute("ErrorRate", ns3::DoubleValue(0.00001));

    ns3::PointToPointHelper pointToPoint;
    pointToPoint.SetDeviceAttribute("DataRate", ns3::StringValue("6Mbps"));
    pointToPoint.SetChannelAttribute("Delay", ns3::StringValue("2ms"));
    pointToPoint.SetDeviceAttribute("ReceiveErrorModel", ns3::PointerValue(errorMOdel)) ;

    ns3::NetDeviceContainer p2pDevices;
    p2pDevices = pointToPoint.Install(p2pNodes);

    ns3::NodeContainer wifi_sender_nodes;
    wifi_sender_nodes.Create(senders);

    ns3::Ptr<ns3::PropagationLossModel> rangeLossModel = ns3::CreateObject<ns3::RangePropagationLossModel>();
    rangeLossModel->SetAttribute("MaxRange", ns3::StringValue(coverageArea));

    ns3::NodeContainer wifi_rcvr_nodes;
    wifi_rcvr_nodes.Create(receivers);

    ns3::YansWifiChannelHelper ch = ns3::YansWifiChannelHelper::Default();
    ns3::YansWifiPhyHelper phy;

    ns3::Ptr<ns3::YansWifiChannel>chan = ch.Create();
    chan->SetPropagationLossModel(rangeLossModel);
    phy.SetChannel(chan);

    ns3::WifiMacHelper mac;
    ns3::Ssid ssid = ns3::Ssid("ns-3-ssid");

    ns3::WifiHelper wifi;

    ns3::NetDeviceContainer staSenderDevices;
    mac.SetType("ns3::StaWifiMac", "Ssid", ns3::SsidValue(ssid), "ActiveProbing", ns3::BooleanValue(false));
    staSenderDevices = wifi.Install(phy, mac, wifi_sender_nodes);

    ns3::NetDeviceContainer apDevices;

    mac.SetType("ns3::ApWifiMac", "Ssid", ns3::SsidValue(ssid));
    apDevices = wifi.Install(phy, mac, p2pNodes.Get(0));

    ssid = ns3::Ssid("ns-3-ssid2");
    phy.SetChannel(ch.Create());

    ns3::NetDeviceContainer staReceiverDevices;
    mac.SetType("ns3::StaWifiMac", "Ssid", ns3::SsidValue(ssid), "ActiveProbing", ns3::BooleanValue(false));
    staReceiverDevices = wifi.Install(phy, mac, wifi_rcvr_nodes);

    mac.SetType("ns3::ApWifiMac", "Ssid", ns3::SsidValue(ssid));
    apDevices.Add(wifi.Install(phy, mac, p2pNodes.Get(1)));

    ns3::MobilityHelper mobility;

    mobility.SetPositionAllocator("ns3::GridPositionAllocator",
                                  "MinX",
                                  ns3::DoubleValue(0.0),
                                  "MinY",
                                  ns3::DoubleValue(0.0),
                                  "DeltaX",
                                  ns3::DoubleValue(0.1),
                                  "DeltaY",
                                  ns3::DoubleValue(0.1),
                                  "GridWidth",
                                  ns3::UintegerValue(40),
                                  "LayoutType",
                                  ns3::StringValue("RowFirst"));

    std::ostringstream model_speed ;

    model_speed << "ns3::ConstantRandomVariable[Constant=" << speed << "]";

    if(!mobile) mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel") ;
    else  {
        mobility.SetMobilityModel("ns3::RandomWalk2dMobilityModel",
                                  "Bounds",
                                  ns3::RectangleValue(ns3::Rectangle(-50, 50, -50, 50)),
                                  "Speed",
                                  ns3::StringValue(model_speed.str()));
    }

    mobility.Install(wifi_sender_nodes);
    mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
    mobility.Install(p2pNodes.Get(0));


    mobility.SetPositionAllocator("ns3::GridPositionAllocator",
                                  "MinX",
                                  ns3::DoubleValue(250),
                                  "MinY",
                                  ns3::DoubleValue(0.0),
                                  "DeltaX",
                                  ns3::DoubleValue(0.1),
                                  "DeltaY",
                                  ns3::DoubleValue(0.1),
                                  "GridWidth",
                                  ns3::UintegerValue(40),
                                  "LayoutType",
                                  ns3::StringValue("RowFirst"));

    if(!mobile) mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel") ;

    else  {
        mobility.SetMobilityModel("ns3::RandomWalk2dMobilityModel",
                                  "Bounds",
                                  ns3::RectangleValue(ns3::Rectangle(200, 300, -50, 50)),
                                  "Speed",
                                  ns3::StringValue(model_speed.str()));
    }
    
    
    
    mobility.Install(wifi_rcvr_nodes);
    mobility.SetMobilityModel("ns3::ConstantPositionMobilityModel");
    mobility.Install(p2pNodes.Get(1));

    ns3::InternetStackHelper stack;
    stack.Install(wifi_sender_nodes);
    stack.Install(wifi_rcvr_nodes);
    stack.Install(p2pNodes);

    ns3::Ipv4AddressHelper address;

    ns3::Ipv4InterfaceContainer p2pInterfaces, staReceiverInterfaces, staSenderInterfaces;

    address.SetBase("10.1.1.0", "255.255.255.0");
    p2pInterfaces = address.Assign(p2pDevices);

    address.SetBase("10.1.2.0", "255.255.255.0");
    staReceiverInterfaces = address.Assign(staReceiverDevices);
    address.Assign(apDevices.Get(1));

    address.SetBase("10.1.3.0", "255.255.255.0");
    staSenderInterfaces = address.Assign(staSenderDevices);
    address.Assign(apDevices.Get(0));

    ns3::Ipv4GlobalRoutingHelper::PopulateRoutingTables();

    ns3::ApplicationContainer sinkApp;
    ns3::ApplicationContainer senderApp;

    uint32_t cnt = 0;

    int rcvr_node[senders];
    int rcvr_port[senders];

    uint16_t i = 0 ;

    for(i = 0 ; i < senders ; i++) {
        rcvr_port[i] = i * 1000 + 5000 ;
        rcvr_node[i] = i ; 
    }

    uint16_t sender = 0;

    std::vector<ns3::Ptr<Flow_helper>> helpers(flows);
    i = 0 ;
    while(i < flows) {
        helpers[i] = ns3::CreateObject<Flow_helper>() ;
        helpers[i]->pay_load_size = pay_load_size;
        i++ ;
    }

    while (cnt != flows){   

        /* Install TCP Receiver on the access point */
        ns3::PacketSinkHelper sinkHelper("ns3::TcpSocketFactory", ns3::InetSocketAddress(
            ns3::Ipv4Address::GetAny(), rcvr_port[sender]));

        ns3::Ptr<ns3::PacketSink> sink; //!< Pointer to the packet sink application
        ns3::ApplicationContainer newSinkApp = sinkHelper.Install(wifi_rcvr_nodes.Get(rcvr_node[sender]));

        sink = ns3::StaticCast<ns3::PacketSink>(newSinkApp.Get(0));

        ns3::Ptr<ns3::Object> theObject = sink;
        theObject->TraceConnectWithoutContext("Rx", MakeBoundCallback(&pkt_rcvd, helpers[cnt]));

        ns3::OnOffHelper sender_helper("ns3::TcpSocketFactory",(ns3::InetSocketAddress(staReceiverInterfaces.GetAddress(rcvr_node[sender]) , rcvr_port[sender])));


        sender_helper.SetAttribute("OnTime", ns3::StringValue("ns3::ConstantRandomVariable[Constant=1]"));
        sender_helper.SetAttribute("OffTime", ns3::StringValue("ns3::ConstantRandomVariable[Constant=0]"));
        sender_helper.SetAttribute("PacketSize", ns3::UintegerValue(pay_load_size));
        sender_helper.SetAttribute("DataRate", ns3::StringValue(dataRate));

        ns3::ApplicationContainer newSenderApp = sender_helper.Install(wifi_sender_nodes.Get(sender));

        theObject = ns3::StaticCast<ns3::OnOffApplication>(newSenderApp.Get(0));
        theObject->TraceConnectWithoutContext("Tx", MakeBoundCallback(&pkt_sent, helpers[cnt]));

        sinkApp.Add(newSinkApp);
        senderApp.Add(newSenderApp);        
        rcvr_node[sender] = (rcvr_node[sender] + 1 )% receivers;
        rcvr_port[sender]+=1 ;
        sender = (sender + 1 )% senders ;
        cnt+=1 ;
    }

    sinkApp.Start(ns3::Seconds(0.0)) ;
    senderApp.Start(ns3::Seconds(1.0)) ;

    return helpers ;
}

ns3::AsciiTraceHelper asciiTraceHelper;
std::string throughputFileName = "";
std::string dlvery_ratioFileName = "";
std::string varParam = "";
int args[10] ;

uint32_t nodes = 20;
uint32_t flows = 10;
uint32_t pay_load_size = 1 << 10 ; /* Transport layer payload size in bytes. */
uint32_t packetsPerSec = 100;
uint16_t simulationTime = 10;
uint32_t coverageArea = 30;
int param_variation = 0;
uint64_t bigNUm = 1e6 ;
uint16_t speed = 5 ;
uint16_t txRange = 5 ;
int param ;


void init(std::string throughputFileName, std:: string varParam, std::string dlvery_ratioFileName,std::ostream*& outputStream1 ,std::ostream*& outputStream2 ,
ns3::Ptr<ns3::OutputStreamWrapper>&stream1,
ns3::Ptr<ns3::OutputStreamWrapper>&stream2) {
    stream1 = asciiTraceHelper.CreateFileStream(throughputFileName + ".dat");
    stream2 = asciiTraceHelper.CreateFileStream(dlvery_ratioFileName + ".dat");
        
    outputStream1 = stream1->GetStream();
    outputStream2 = stream2->GetStream();
}

void 
common(uint16_t flows,std::vector<ns3::Ptr<Flow_helper>>&helper, int i,
std::ostream*& outputStream1,
std::ostream*& outputStream2){

    ns3::Simulator::Stop(ns3::Seconds(simulationTime + 1));
    ns3::Simulator::Run();
    ns3::Simulator::Destroy() ;
    std::cout << "total Bytes Sent : " << tot_bytes_sent << "\n"
              << "total Bytes Received : " << tot_bytes_rcvd << "\n";

    uint64_t tot_bits_rcvd = 0 ,tot_pkt_rcvd = 0 , tot_pkt_sent = 0;
    float dlvery_ratio = 0.0 ;

    for (uint16_t i = 0; i < flows ; i++) {
        tot_bits_rcvd = tot_bits_rcvd + helper[i]->tot_bits_rcvd ;
        dlvery_ratio += helper[i]->tot_pkt_rcvd / (float) helper[i]->tot_pkt_sent ;
        tot_pkt_rcvd = tot_pkt_rcvd + helper[i]->tot_pkt_rcvd ;
        tot_pkt_sent = tot_pkt_sent + helper[i]->tot_pkt_sent ;
    }

    float avg_thput = tot_bits_rcvd / (simulationTime * (double) 1e6);
    dlvery_ratio /= flows;

    std::cout << "avg Throughput : " << avg_thput << " Mbit/s\n";
    std::cout << "delivery Ratio : " << dlvery_ratio << "\n";

    *outputStream1 << args[i] << "  " << avg_thput << "\n";
    *outputStream2 << args[i] << "  " << dlvery_ratio << "\n";

    tot_bytes_rcvd = tot_bytes_sent = 0;
}

int
main(int argc, char* argv[])
{

    ns3::Ptr<ns3::OutputStreamWrapper> stream1 ;
    ns3::Ptr<ns3::OutputStreamWrapper>stream2 ;
    std::ostream* outputStream1;
    std::ostream* outputStream2;
    int mobile = 0 ;
    for(int j = 0 ; j < 10 ; j++) args[j] = -1 ;
    ns3::CommandLine cmd(__FILE__);
    cmd.AddValue("varParam","parameter which is going to be varied. Valid parameters : 0 - 4",param_variation);

    for (int i = 0; i < 10; i++) {
        cmd.AddValue("args" + std::to_string((i + 1)), "values of varying parameter...must be greater than 0", args[i]);
    }
    cmd.AddValue("mobile", "mobile or static...valid inputs : 0/1", mobile);

    //parse the args
    cmd.Parse(argc, argv);

    if (param_variation >= 5 or param_variation < 0) {
        NS_LOG_UNCOND("Invalid varying Parameter");
        exit(1);
    }

    if (mobile != 0 && mobile != 1) {
        NS_LOG_UNCOND("Invalid mobile static parameter");
        exit(1);
    }

    bool is_mobile ;
    is_mobile = !mobile ? false : true ;
    coverageArea = is_mobile ? 150 :coverageArea ;

    param = param_variation ;
    

    for (int i = 0; i < 5; i++) {
        if(args[i] >= -1) continue ;
        NS_LOG_UNCOND("Invalid value of varying-Parameter");
        exit(1);
    }

    if (param == NODE) {
        for( int i = 5 ; i < 10 ; i++) {
            if (args[i] >= -1) continue ;
            NS_LOG_UNCOND("Invalid value of varying-Parameter");
            exit(1);
            
        }
        varParam = "Nodes";
        if (is_mobile) {
            throughputFileName = "throughput_mobile_variableNodes";
            dlvery_ratioFileName = "deliveryRatio_mobile_variableNodes";
        }
        else {
            throughputFileName = "throughput_static_variableNodes";
            dlvery_ratioFileName = "deliveryRatio_static_variableNodes";
        }

        init(throughputFileName,varParam,dlvery_ratioFileName,outputStream1,outputStream2,
        stream1,stream2) ;

        for (int i = 0; i < 10; i += 2)
        {
            uint16_t nodes = args[i] ;
            uint16_t flows = args[i + 1];

            if (nodes <= 1200) {
                std::vector<ns3::Ptr<Flow_helper>>helper = static_solve(args[i],flows,packetsPerSec, coverageArea,pay_load_size,is_mobile,speed);
                std::cout << "nodes : " << nodes <<'\n' ;
                common(flows,helper,i,outputStream1,outputStream2) ;
            }
        }

    }
    else if (param == FLOW) {
        std::string s = is_mobile ? "_mobile_" : "_static_" ;
        throughputFileName = "throughput" + s +"variableFlow";
        varParam = "Flows";

        dlvery_ratioFileName = "dlvery_ratio"+s+"variableFlow";
                init(throughputFileName,varParam,dlvery_ratioFileName,outputStream1,outputStream2,
        stream1,stream2) ;

        for (int i = 0; i < 5; i++) {
            uint16_t flows = args[i];
            std::vector<ns3::Ptr<Flow_helper>>helper = static_solve(nodes,args[i], packetsPerSec, coverageArea, pay_load_size,is_mobile,speed);
            std::cout << "flows : " << flows <<'\n' ;
            common(flows,helper,i,outputStream1,outputStream2) ;
        }
    }
    else if (param == PCKTSPERSEC) {
        std::string s = is_mobile ? "_mobile_" : "_static_" ;
        throughputFileName = "throughput"+s+"variablePacketsPerSec";
        varParam = "Packets Per Second";
        dlvery_ratioFileName = "dlvery_ratio"+s+"variablePacketsPerSec";

                init(throughputFileName,varParam,dlvery_ratioFileName,outputStream1,outputStream2,
        stream1,stream2) ;

        for (int i = 0; i < 5; i++) {
            std::vector<ns3::Ptr<Flow_helper>> helper = static_solve(nodes, flows,args[i], coverageArea,pay_load_size,is_mobile,speed);
            std::cout << "Packets persec : " << args[i] <<'\n' ;
            common(flows,helper,i,outputStream1,outputStream2) ;

        }
    }
    else if (param == COVERAGEAREA && !is_mobile)
    {
        varParam = "Coverage Area";
        std::string s = is_mobile ? "_mobile_" : "_static_" ;

        throughputFileName = "throughput"+s+"variableCoverageArea";
        dlvery_ratioFileName = "dlvery_ratio"+s+"variableCoverageArea";

                init(throughputFileName,varParam,dlvery_ratioFileName,outputStream1,outputStream2,
        stream1,stream2) ;

        for (int i = 0; i < 5; i++) {
            uint32_t z = args[i] * txRange ;
            std::vector<ns3::Ptr<Flow_helper>>helper = static_solve(nodes, flows,packetsPerSec, z, pay_load_size,is_mobile,speed);
            std::cout << "Coverage Area : " << args[i] * txRange <<'\n' ;
            common(flows,helper,i,outputStream1,outputStream2) ;
        }
    }

    else if(param == NODESPEED && is_mobile) {
        varParam = "speed" ;
        std::string s = is_mobile ? "_mobile_" : "_static_" ;
        throughputFileName = "throughput"+s+"variableSpeed";
        dlvery_ratioFileName = "deliveryRatio"+s+"variableSpeed";

                init(throughputFileName,varParam,dlvery_ratioFileName,outputStream1,outputStream2,
        stream1,stream2) ;

        for (int i = 0; i < 5; i++) {
            std::vector<ns3::Ptr<Flow_helper>> helper = static_solve(nodes, flows,packetsPerSec , coverageArea ,pay_load_size,is_mobile,args[i]);
            std::cout << "Speed : " << args[i] << " m/s\n";
            common(flows,helper,i,outputStream1,outputStream2) ;

        }

    }

    stream1 = asciiTraceHelper.CreateFileStream(throughputFileName + ".plt");
    outputStream1 = stream1->GetStream() ; 

    *outputStream1 << "set terminal png size 640,480\n"
                    << "set output \"" << throughputFileName << ".png\"\n"
                    << "plot \"" << throughputFileName << ".dat\" using 1:2 title 'Throughput VS "
                   << varParam << "' with linespoints\n";

    stream2 = asciiTraceHelper.CreateFileStream(dlvery_ratioFileName + ".plt");
    outputStream2 = stream2->GetStream() ;

    *outputStream2 << "set terminal png size 640,480\n"
                  << "set output \"" << dlvery_ratioFileName << ".png\"\n" 
                    << "plot \"" << dlvery_ratioFileName << ".dat\" using 1:2 title 'DeliveryRatio VS "
                   << varParam << "' with linespoints\n";

    return 0;
}