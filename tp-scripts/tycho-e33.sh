DIR=/home/j2ee-hudson
TPDIR=$DIR/tycho-tp-e33
rm -rf $TPDIR
mkdir -p $TPDIR

# eclipse 3.3.2
wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.3.2-200802211800/eclipse-SDK-3.3.2-linux-gtk-x86_64.tar.gz

# RCP delta pack
wget -P $TPDIR  -N http://archive.eclipse.org/eclipse/downloads/drops/R-3.3.2-200802211800/eclipse-RCP-3.3.2-delta-pack.zip



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

#cd eclipse
#for i in ../sites/*.zip
#do 
#  unzip -o $i 
#done

