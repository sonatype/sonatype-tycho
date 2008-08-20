DIR=/home/j2ee-hudson
TPDIR=$DIR/eclipse-tp-e32a
rm -rf $TPDIR
mkdir -p $TPDIR

# eclipse 3.2.2
#wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-SDK-3.2.2-win32.zip
##wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-platform-3.2.2-win32.zip
##wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-JDT-3.2.2.zip
##wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-PDE-SDK-3.2.2.zip
wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-SDK-3.2.2-linux-gtk-x86_64.tar.gz

# wtp&co 1.5
#wget -P $TPDIR  -N http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.3.2/R200802051830/emf-sdo-xsd-SDK-2.3.2.zip
#wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.3.2/R200802211602/GEF-SDK-3.3.2.zip
#wget -P $TPDIR  -N http://download.eclipse.org/datatools/downloads/1.5/dtp-sdk_1.5.2_022008.zip
#wget -P $TPDIR  -N http://download.eclipse.org/webtools/downloads/drops/R2.0/R-2.0.3-20080710044639/wtp-sdk-R-2.0.3-20080710044639.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/webtools/downloads/drops/R1.5/R-1.5.5-200708291442/wtp-R-1.5.5-200708291442.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/webtools/downloads/drops/R1.5/R-1.5.5-200708291442/wtp-jem-sdk-R-1.5.5-200708291442.zip
wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/webtools/downloads/drops/R1.5/R-1.5.5-200708291442/wtp-wst-R-1.5.5-200708291442.zip
wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/webtools/downloads/drops/R1.5/R-1.5.5-200708291442/wtp-jem-R-1.5.5-200708291442.zip

# emf-sdo-xsd-SDK-M200708282030.zip
# emf-sdo-xsd-SDK-2.2.2.zip
# wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.2.2/R200702131851/emf-sdo-xsd-SDK-2.2.2.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.3.1/R200709252135/emf-sdo-xsd-SDK-2.3.1.zip
wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.3.1/R200709252135/emf-sdo-runtime-2.3.1.zip
wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.3.1/R200709252135/xsd-runtime-2.3.1.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.2.2/R200702131851/emf-sdo-runtime-2.2.2.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.2.2/R200702131851/xsd-runtime-2.2.2.zip
# emf-sdo-xsd-SDK-2.2.4.zip
# wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/modeling/emf/emf/downloads/drops/2.2.4/R200710030400/emf-sdo-xsd-SDK-2.2.4.zip

# GEF-SDK-3.2.2.zip
##wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/tools/gef/downloads/drops/R-3.2.2-200702081315/GEF-SDK-3.2.2.zip
wget -P $TPDIR  -N http://www.eclipse.org/downloads/download.php?file=/tools/gef/downloads/drops/R-3.2.2-200702081315/GEF-runtime-3.2.2.zip

# zest 3.4 GA
##wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.4.0/R200806091334/GEF-zest-sdk-3.4.0.zip
wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.4.0/R200806091334/GEF-zest-3.4.0.zip

# AJDT 1.5.3
# wget -P $TPDIR/sites -N http://download.eclipse.org/tools/ajdt/33/update/ajdt_1.5.3_for_eclipse_3.3.zip

# subclipse 1.4.3
wget -P $TPDIR/sites  -N http://subclipse.tigris.org/files/documents/906/43326/site-1.4.3.zip

# mylyn 2.0
# wget -P $TPDIR/sites -N http://download.eclipse.org/tools/mylyn/update-archive/2.3.2/v20080402-2100/e3.3/mylyn-2.3.2.v20080402-2100-e3.3.zip
# wget -P $TPDIR  -N -P sites http://download.eclipse.org/tools/mylyn/update/mylyn-3.0.1-e3.3.zip

# windows tester 3.5.0
wget -P $TPDIR/sites  -N http://download.instantiations.com/WindowTesterPro/release/v3.5.0_200806270350/WindowTesterPro_v3.5.0_for_Eclipse3.2.zip

#############################################################
#############################################################
#############################################################

cd $TPDIR

tar xfz *.tar.gz

for i in *.zip
do 
  unzip -o $i 
done

cd eclipse
for i in ../sites/*.zip
do 
  unzip -o $i -x site.xml
done
