#TPDIR=$DIR/eclipse-tp-e34
#rm -rf $TPDIR
#mkdir -p $TPDIR

TPDIR=.

MIRROR=http://download.eclipse.org
#MIRROR=http://mirror.csclub.uwaterloo.ca/eclipse
#MIRROR=http://gulus.USherbrooke.ca/pub/appl/eclipse

# eclipse 3.4.2
PLATFORM_ARCHIVE=eclipse-SDK-3.4.2-linux-gtk-x86_64.tar.gz
wget -P $TPDIR  -N $MIRROR/eclipse/downloads/drops/R-3.4.2-200902111700/$PLATFORM_ARCHIVE

# wtp&co 3.0.4
wget -P $TPDIR  -N $MIRROR/modeling/emf/emf/downloads/drops/2.4.2/R200902171115/emf-runtime-2.4.2.zip
wget -P $TPDIR  -N $MIRROR/modeling/emf/emf/downloads/drops/2.4.2/R200902171115/xsd-runtime-2.4.2.zip
wget -P $TPDIR  -N $MIRROR/tools/gef/downloads/drops/3.4.2/R200902171642/GEF-SDK-3.4.2.zip
wget -P $TPDIR  -N $MIRROR/datatools/downloads/1.6/dtp-sdk_1.6.2.zip
wget -P $TPDIR  -N $MIRROR/webtools/downloads/drops/R3.0/R-3.0.4-20090213193639/wtp-sdk-R-3.0.4-20090213193639.zip

# zest 3.4 GA
wget -P $TPDIR  -N $MIRROR/tools/gef/downloads/drops/3.4.2/R200902171642/GEF-zest-sdk-3.4.2.zip

# AJDT 1.6.0
wget -P $TPDIR/sites  -N $MIRROR/tools/ajdt/34/update/ajdt_1.6.4_for_eclipse_3.4.zip

# subclipse 1.4.3
#wget -P $TPDIR/sites -N http://subclipse.tigris.org/files/documents/906/45156/site-1.4.8.zip

# subversive
wget -P $TPDIR/sites  -N $MIRROR/technology/subversive/0.7/builds/Subversive-headless-incubation-0.7.7.I20090224-1900.zip

# mylyn
#wget -P $TPDIR/sites -N $MIRROR/tools/mylyn/update-archive/2.3.2/v20080402-2100/e3.4/mylyn-2.3.2.v20080402-2100-e3.4.zip
wget -P $TPDIR/sites -N $MIRROR/tools/mylyn/update/mylyn-3.1.0-e3.4.zip

# windows tester 3.5.0
wget -P $TPDIR/sites  -N http://download.instantiations.com/WindowTesterPro/release/v3.7.0_200901230704/WindowTesterPro_v3.7.0_for_Eclipse3.4.zip


#############################################################
#############################################################
#############################################################

cd $TPDIR

FILES=`pwd`
ECLIPSE=`pwd`/eclipse
DROPINS=$ECLIPSE/dropins

rm -rf $ECLIPSE

# install
tar xfz $FILES/$PLATFORM_ARCHIVE

# install everything under dropins 
# eclipse does not immediately scan plugins folder for new bundles

for i in $FILES/*.zip
do
  DIR=$DROPINS/`basename $i .zip`
  mkdir -p $DIR
  cd $DIR
  touch .eclipseextension
  unzip -o $i
done

for i in $FILES/sites/*.zip
do 
  DIR=$DROPINS/`basename $i .zip`/eclipse
  mkdir -p $DIR
  cd $DIR
  unzip -o $i

  touch ../.eclipseextension

  # p2 explicitely checks for site.xml
  rm -f site.xml

  # unzip all features
  for j in $DIR/features/*.jar
  do
    FDIR=$DIR/features/`basename $j .jar`
    mkdir -p $FDIR
    cd $FDIR
    unzip $j
    rm -f $j ${j}.pack.gz
  done
  rm $DIR/site.xml
done
