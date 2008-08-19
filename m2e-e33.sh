DIR=`pwd`
TPDIR=$DIR/eclipse-tp-e33
rm -rf $TPDIR
mkdir -p $TPDIR

# eclipse 3.3.2
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.3.2-200802211800/eclipse-SDK-3.3.2-linux-gtk-x86_64.tar.gz

# wtp&co 2.0.3
wget -P $TPDIR  -N http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.3.2/R200802051830/emf-sdo-xsd-SDK-2.3.2.zip
wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.3.2/R200802211602/GEF-SDK-3.3.2.zip
wget -P $TPDIR  -N http://download.eclipse.org/datatools/downloads/1.5/dtp-sdk_1.5.2_022008.zip
wget -P $TPDIR  -N http://download.eclipse.org/webtools/downloads/drops/R2.0/R-2.0.3-20080710044639/wtp-sdk-R-2.0.3-20080710044639.zip


# zest 3.4 GA
wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.4.0/R200806091334/GEF-zest-sdk-3.4.0.zip

# AJDT 1.5.3
wget -P $TPDIR  -N -P sites http://download.eclipse.org/tools/ajdt/33/update/ajdt_1.5.3_for_eclipse_3.3.zip

# subclipse 1.4.3
wget -P $TPDIR  -N -P sites http://subclipse.tigris.org/files/documents/906/43326/site-1.4.3.zip

# mylyn 3.0.1
wget -P $TPDIR  -N -P sites  http://download.eclipse.org/tools/mylyn/update-archive/2.3.2/v20080402-2100/e3.3/mylyn-2.3.2.v20080402-2100-e3.3.zip
#wget -P $TPDIR  -N -P sites http://download.eclipse.org/tools/mylyn/update/mylyn-3.0.1-e3.3.zip

# windows tester 3.5.0
wget -P $TPDIR  -N -P sites http://download.instantiations.com/WindowTesterPro/release/v3.5.0_200806270350/WindowTesterPro_v3.5.0_for_Eclipse3.3.zip

#############################################################
#############################################################
#############################################################

cd $TPDIR

# install
tar xfz *.tar.gz
for i in *.zip
do 
  unzip -o $i 
done

cd eclipse
for i in ../sites/*.zip
do 
  unzip -o $i 
done

