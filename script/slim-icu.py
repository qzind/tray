#slim ICU
import zipfile
from zipfile import ZipFile
import zlib
mode= zipfile.ZIP_DEFLATED

def keep_file(filename):
    #skip all break iterators
    if filename.endswith(".brk"):
        return False
    if filename.endswith(".dict"):
        return False
    if filename.endswith("unames.icu"):
        return False
    if filename.endswith("ucadata.icu"):
        return False
    if filename.endswith(".spp"):
        return False

    #keep english and arabic
    if filename.startswith("en") or filename.startswith("ar"):
        return True

    if not filename.endswith(".res"):
        return True

    return False

zin = ZipFile ('icu4j.jar', 'r')
zout = ZipFile ('icu4j-slim.jar', 'w', mode)

for item in zin.infolist():
    buff = zin.read(item.filename)
    print item.filename,
    if (keep_file(item.filename)):
        print "Keep"
        zout.writestr(item, buff)
    else:
        print "Remove"

zout.close()
zin.close()

def keep_charset_file(filename):

	to_remove = ["cns-11643-1992.cnv", "ebcdic-xml-us.cnv", "euc-jp-2007.cnv", "euc-tw-2014.cnv", "gb18030.cnv", "ibm-1363_P11B-1998.cnv", "ibm-1364_P110-2007.cnv", "ibm-1371_P100-1999.cnv", "ibm-1373_P100-2002.cnv", "ibm-1375_P100-2008.cnv", "ibm-1383_P110-1999.cnv", "ibm-1386_P100-2001.cnv", "ibm-1388_P103-2001.cnv", "ibm-1390_P110-2003.cnv"]

	for i in to_remove:
		if i in filename:
			return False

	return True



zin = ZipFile ('icu4j-charset.jar', 'r')
zout = ZipFile ('icu4j-charset-slim.jar', 'w', mode)

for item in zin.infolist():
    buff = zin.read(item.filename)
    print item.filename,
    if (keep_charset_file(item.filename)):
        print "Keep"
        zout.writestr(item, buff)
    else:
        print "Remove"

zout.close()
zin.close()
