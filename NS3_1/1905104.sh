
./ns3 run "scratch/1905104.cc --varParam=0 --args1=20 --args2=10 --args3=40 --args4=20 --args5=60 --args6=30 --args7=100 --args8=40 --args9=100 --args10=50"
gnuplot throughput_static_variableNodes.plt
gnuplot deliveryRatio_static_variableNodes.plt


./ns3 run "scratch/1905104.cc --varParam=1 --args1=10 --args2=20 --args3=30 --args4=40 --args5=50"
gnuplot throughput_static_variableFlow.plt
gnuplot deliveryRatio_static_variableFlow.plt


./ns3 run "scratch/1905104.cc --varParam=2 --args1=100 --args2=200 --args3=300 --args4=400 --args5=500"
gnuplot throughput_static_variablePacketsPerSec.plt
gnuplot deliveryRatio_static_variablePacketsPerSec.plt

./ns3 run "scratch/1905104.cc --varParam=3 --args1=1 --args2=2 --args3=3 --args4=4 --args5=5"
gnuplot throughput_static_variableCoverageArea.plt
gnuplot deliveryRatio_static_variableCoverageArea.plt



./ns3 run "scratch/1905104.cc --varParam=0 --mobile=1 --args1=10 --args2=10 --args3=20 --args4=20 --args5=30 --args6=30 --args7=40 --args8=40 --args9=50 --args10=50"
gnuplot throughput_mobile_variableNodes.plt
gnuplot deliveryRatio_mobile_variableNodes.plt


./ns3 run "scratch/1905104.cc --varParam=1 --args1=10 --args2=20 --args3=30 --args4=40 --args5=50 --mobile=1"
gnuplot throughput_mobile_variableFlow.plt
gnuplot deliveryRatio_mobile_variableFlow.plt


./ns3 run "scratch/1905104.cc --varParam=2 --args1=100 --args2=200 --args3=300 --args4=400 --args5=500 --mobile=1"
gnuplot throughput_mobile_variablePacketsPerSec.plt
gnuplot deliveryRatio_mobile_variablePacketsPerSec.plt

./ns3 run "scratch/1905104.cc --varParam=4 --args1=5 --args2=10 --args3=15 --args4=20 --args5=25 --mobile=1"
gnuplot throughput_mobile_variableSpeed.plt
gnuplot deliveryRatio_mobile_variableSpeed.plt