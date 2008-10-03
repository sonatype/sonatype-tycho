DIR=/home/j2ee-hudson
TPDIR=$DIR/tycho-tp-e32
rm -rf $TPDIR
mkdir -p $TPDIR

#TPDIR=.

# eclipse 3.2.2
wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-SDK-3.2.2-linux-gtk-x86_64.tar.gz

# RCP delta pack
wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.2-200702121330/eclipse-RCP-3.2.2-delta-pack.zip


#############################################################
#############################################################
#############################################################

cd $TPDIR

tar xfz *.tar.gz

for i in *.zip
do 
  unzip -o $i 
done

#cd eclipse
#for i in ../sites/*.zip
#do 
#  unzip -o $i -x site.xml
#done
