DIR=/home/j2ee-hudson
TPDIR=$DIR/eclipse-tp-e34
rm -rf $TPDIR
mkdir -p $TPDIR

# eclipse 3.4 GA
# wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4-200806172000/eclipse-SDK-3.4-linux-gtk-x86_64.tar.gz
# wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4-200806172000/eclipse-SDK-3.4-linux-gtk.tar.gz
# eclipse 3.4.1
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-linux-gtk.tar.gz
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-linux-gtk-x86_64.tar.gz
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-win32.zip
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-win32-x86_64.zip
wget -P $TPDIR  -N http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-macosx-carbon.tar.gz

# wtp&co 3.0 GA
wget -P $TPDIR  -N http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.4.0/R200806091234/emf-runtime-2.4.0.zip
wget -P $TPDIR  -N http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.4.0/R200806091234/xsd-runtime-2.4.0.zip
wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.4.0/R200806091334/GEF-SDK-3.4.0.zip
wget -P $TPDIR  -N http://download.eclipse.org/datatools/downloads/1.6/dtp-sdk_1.6.0.zip
wget -P $TPDIR  -N http://download.eclipse.org/webtools/downloads/drops/R3.0/R-3.0-20080616152118/wtp-sdk-R-3.0-20080616152118a.zip

# zest 3.4 GA
wget -P $TPDIR  -N http://download.eclipse.org/tools/gef/downloads/drops/3.4.0/R200806091334/GEF-zest-sdk-3.4.0.zip

# AJDT 1.6.0
wget -P $TPDIR/sites  -N http://download.eclipse.org/tools/ajdt/34/update/ajdt_1.6.0_for_eclipse_3.4.zip

# subclipse 1.4.3
wget -P $TPDIR/sites -N http://subclipse.tigris.org/files/documents/906/43326/site-1.4.3.zip

# mylyn
#wget -P $TPDIR/sites -N http://download.eclipse.org/tools/mylyn/update-archive/2.3.2/v20080402-2100/e3.4/mylyn-2.3.2.v20080402-2100-e3.4.zip
wget -P $TPDIR/sites -N http://download.eclipse.org/tools/mylyn/update-archive/3.0.1/v20080721-2100/mylyn-3.0.1.v20080721-2100-e3.4.zip

# windows tester 3.5.0
wget -P $TPDIR/sites  -N http://download.instantiations.com/WindowTesterPro/release/v3.5.0_200806270350/WindowTesterPro_v3.5.0_for_Eclipse3.4.zip


#############################################################
#############################################################
#############################################################

cd $TPDIR

FILES=`pwd`
DROPINS=`pwd`/eclipse/dropins

# install
tar xfz $FILES/eclipse-SDK-3.4-linux-gtk.tar.gz
tar xfz $FILES/eclipse-SDK-3.4-linux-gtk-x86_64.tar.gz

# install everything under dropts 
# eclipse does not immediately scan plugins folder for new bundles

for i in $FILES/*.zip
do
  DIR=$DROPINS/`basename $i .zip`
  mkdir -p $DIR
  cd $DIR
  unzip -o $i 
done

for i in $FILES/sites/*.zip
do 
  DIR=$DROPINS/`basename $i .zip`/eclipse
  mkdir -p $DIR
  cd $DIR
  unzip -o $i 
done

