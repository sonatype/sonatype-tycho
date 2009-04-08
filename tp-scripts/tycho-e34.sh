DIR=/home/j2ee-hudson
TPDIR=$DIR/tycho-tp-e34
rm -rf $TPDIR
mkdir -p $TPDIR

TPDIR=.

MIRROR=http://download.eclipse.org

wget -P $TPDIR  -N $MIRROR/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-linux-gtk-x86_64.tar.gz
wget -P $TPDIR  -N $MIRROR/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-3.4.1-delta-pack.zip


#############################################################
#############################################################
#############################################################

cd $TPDIR

FILES=`pwd`
ECLIPSE=`pwd`/eclipse
DROPINS=$ECLIPSE/dropins

rm -rf $ECLIPSE

# install
tar xfz $FILES/eclipse-SDK-3.4.1-linux-gtk-x86_64.tar.gz

# install everything under dropts 
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
done

