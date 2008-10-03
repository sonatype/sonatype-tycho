DIR=/home/j2ee-hudson
TPDIR=$DIR/tycho-tp-e34
rm -rf $TPDIR
mkdir -p $TPDIR

#TPDIR=.

MIRROR=http://download.eclipse.org

wget -P $TPDIR  -N $MIRROR/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-SDK-3.4.1-linux-gtk-x86_64.tar.gz
wget -P $TPDIR  -N $MIRROR/eclipse/downloads/drops/R-3.4.1-200809111700/eclipse-3.4.1-delta-pack.zip


#############################################################
#############################################################
#############################################################

cd $TPDIR

FILES=`pwd`
DROPINS=`pwd`/eclipse/dropins

# cleanup previous install
rm -rf eclipse

# install
tar xfz $FILES/eclipse-SDK-3.4.1-linux-gtk-x86_64.tar.gz

# install everything under dropins 
# eclipse does not immediately scan plugins folder for new bundles

for i in $FILES/*.zip
do
  DIR=$DROPINS/`basename $i .zip`
  mkdir -p $DIR
  cd $DIR
  unzip -o $i 
done

#for i in $FILES/sites/*.zip
#do 
#  DIR=$DROPINS/`basename $i .zip`/eclipse
#  mkdir -p $DIR
#  cd $DIR
#  unzip -o $i 
#done

