import com.github.lukethompsxn.edufuse.filesystem.FileSystemStub;
import com.github.lukethompsxn.edufuse.struct.*;
import com.github.lukethompsxn.edufuse.util.ErrorCodes;
import com.github.lukethompsxn.edufuse.util.FuseFillDir;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.types.dev_t;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import util.MemoryINode;
import util.MemoryINodeTable;
import util.MemoryVisualiser;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import com.sun.security.auth.module.UnixSystem;

/**
 * @author Luke Thompson and William Shin
 * @since 04.09.19
 */
public class MemoryFS extends FileSystemStub {
    private static final String HELLO_PATH = "/hello";
    private static final String HELLO_STR = "Hello World!\n";

    private MemoryINodeTable iNodeTable = new MemoryINodeTable();
    private MemoryVisualiser visualiser;
    private UnixSystem unix = new UnixSystem();
    private Instant instant = Instant.now();

    @Override
    public Pointer init(Pointer conn) {

        // setup an example file for testing purposes
        MemoryINode iNode = new MemoryINode();
        FileStat stat = new FileStat(Runtime.getSystemRuntime());

        System.out.println("init getSystemRuntime: " + Runtime.getSystemRuntime());
        System.out.println("init ctim.tv_sec: " + stat.st_ctim.tv_sec.get());

        // you will have to add more stat information here eventually
        stat.st_mode.set(FileStat.S_IFREG | 0444 | 0200);
        stat.st_size.set(HELLO_STR.getBytes().length);
        stat.st_nlink.set(1);
        stat.st_uid.set(unix.getUid());
        stat.st_gid.set(unix.getGid());

        //stat.st_ctim.set()
        //Timespec ts = new Timespec(Runtime.getSystemRuntime()); 
        //Timespec ts = Timespec.of(conn);

        //Timespec timespec1 = Timespec.of(conn);
        //Timespec timespec2 = Timespec.of(conn.slice(Struct.size(timespec1)));

        // this.utimens(HELLO_PATH, new Timespec[] {timespec1, timespec2});

       // stat.st_ctim.tv_sec.set(ts.tv_sec.get());
        //stat.st_ctim.tv_nsec.set(ts.tv_nsec.longValue());
        stat.st_ctim.tv_sec.set(instant.getEpochSecond());
        stat.st_ctim.tv_nsec.set(instant.getNano());
        stat.st_mtim.tv_sec.set(instant.getEpochSecond());
        stat.st_mtim.tv_nsec.set(instant.getNano());
        stat.st_atim.tv_sec.set(instant.getEpochSecond());
        stat.st_atim.tv_nsec.set(instant.getNano());

        //stat.st_ctim.tv_sec.set(timespec1.tv_sec.get());
        //stat.st_ctim.tv_nsec.set(timespec1.tv_nsec.longValue());


        System.out.println("init ctim.tv_sec: " + stat.st_ctim.tv_sec.get());
        System.out.println("init ctim.tv_nsec: " + stat.st_ctim.tv_nsec.longValue());
        //this.utimens(HELLO_PATH, ts);
        

        //stat.st_ctim.set(FileStat.of(conn).st_ctim);
        //stat.st_ctim.set(new Timespec(stat.getRuntime()));
        //stat.st_ctim.tv_sec = Runtime.getSystemRuntime();



        iNode.setStat(stat);
        iNode.setContent(HELLO_STR.getBytes());
        iNodeTable.updateINode(HELLO_PATH, iNode);
        //this.utimens(HELLO_PATH, new Timespec[] {timespec1, timespec2});

        if (isVisualised()) {
            visualiser = new MemoryVisualiser();
            visualiser.sendINodeTable(iNodeTable);
        }

        return conn;
    }

    @Override
    public int getattr(String path, FileStat stat) {
        int res = 0;
        if (Objects.equals(path, "/")) { // minimal set up for the mount point root
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
        } else if (iNodeTable.containsINode(path)) {
            FileStat savedStat = iNodeTable.getINode(path).getStat();
            // fill in the stat object with values from the savedStat object of your inode
            stat.st_mode.set(savedStat.st_mode.intValue());
            stat.st_size.set(savedStat.st_size.intValue());
            stat.st_dev.set(savedStat.st_dev.intValue());
            stat.st_ino.set(savedStat.st_ino.intValue());
            stat.st_nlink.set(savedStat.st_nlink.intValue());
            stat.st_uid.set(savedStat.st_uid.intValue());
            stat.st_gid.set(savedStat.st_gid.intValue());
            stat.st_rdev.set(savedStat.st_rdev.intValue());
            stat.st_blksize.set(savedStat.st_blksize.intValue());
            stat.st_blocks.set(savedStat.st_blocks.intValue());
            stat.st_atim.tv_sec.set(savedStat.st_atim.tv_sec.intValue());
            stat.st_atim.tv_nsec.set(savedStat.st_atim.tv_nsec.intValue());
            stat.st_mtim.tv_sec.set(savedStat.st_mtim.tv_sec.intValue());
            stat.st_mtim.tv_nsec.set(savedStat.st_mtim.tv_nsec.intValue());

            System.out.println("ctim.tv_sec: " + savedStat.st_ctim.tv_sec.get());
            stat.st_ctim.tv_sec.set(savedStat.st_ctim.tv_sec.get());
            stat.st_ctim.tv_nsec.set(savedStat.st_ctim.tv_nsec.longValue());


        } else {
            res = -ErrorCodes.ENOENT();
        }
        return res;
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo fi) {
        // For each file in the directory call filler.apply.
        // The filler.apply method adds information on the files
        // in the directory, it has the following parameters:
        //      buf - a pointer to a buffer for the directory entries
        //      name - the file name (with no "/" at the beginning)
        //      stbuf - the FileStat information for the file
        //      off - just use 0

        for (String fileName: this.iNodeTable.entries()) {
            System.out.println(fileName);
            filler.apply(buf, fileName.substring(1), this.iNodeTable.getINode(fileName).getStat(), 0);
        }

        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int flush(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        if (!iNodeTable.containsINode(path)) {
            return -ErrorCodes.ENOENT();
        }
        // you need to extract data from the content field of the inode and place it in the buffer
        // something like:
        // buf.put(0, content, offset, amount);
        int amount = this.iNodeTable.getINode(path).getContent().length;

        
        System.out.println("reading path: " + path);
        System.out.println("reading size: " + size);
        System.out.println("reading offset: " + offset);
        System.out.println("content length: " + amount);


        //buf.put(offset, src, idx, len);
        buf.put(0, this.iNodeTable.getINode(path).getContent(), (int) offset, amount);

        if (isVisualised()) {
            visualiser.sendINodeTable(iNodeTable);
        }

        return amount;
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        if (!iNodeTable.containsINode(path)) {
            return -ErrorCodes.ENOENT(); // ENONET();
        }
        // similar to read but you get data from the buffer like:
        // buf.get(0, content, offset, size);
        //buf.get(0, this.iNodeTable.)

        if (isVisualised()) {
            visualiser.sendINodeTable(iNodeTable);
        }

        return (int) size;
    }

    @Override
    public int mknod(String path, @mode_t long mode, @dev_t long rdev) {
        if (iNodeTable.containsINode(path)) {
            return -ErrorCodes.EEXIST();
        }

        MemoryINode mockINode = new MemoryINode();
        // set up the stat information for this inode

        iNodeTable.updateINode(path, mockINode);

        if (isVisualised()) {
            visualiser.sendINodeTable(iNodeTable);
        }

        return 0;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        return super.statfs(path, stbuf);
    }

    @Override
    public int utimens(String path, Timespec[] timespec) {
        // The Timespec array has the following information.
        // You need to set the corresponding fields of the inode's stat object.
        // You can access the data in the Timespec objects with "get()" and "longValue()".
        // You have to find out which time fields these correspond to.
        // timespec[0].tv_nsec
        // timespec[0].tv_sec
        // timespec[1].tv_nsec
        // timespec[1].tv_sec

        // Timespec timespec1 = Timespec.of(timespec);
        // Timespec timespec2 = Timespec.of(timespec.slice(Struct.size(timespec1)));

        System.out.println(timespec[0].tv_nsec.longValue());
        System.out.println(timespec[0].tv_sec.get());
        System.out.println(timespec[1].tv_nsec.longValue());
        System.out.println(timespec[1].tv_sec.get());

        // stat.st_ctim.tv_sec.set(ts.tv_sec.get());
        // this.iNodeTable.getINode(path).getStat().st_ctim.tv_sec.set(timespec[1].tv_sec.get());
        // this.iNodeTable.getINode(path).getStat().st_ctim.tv_nsec.set(timespec[1].tv_nsec.longValue());

        return 0;
    }
    
    @Override
    public int link(java.lang.String oldpath, java.lang.String newpath) {
        return 0;
    }

    @Override
    public int unlink(String path) {
        if (!iNodeTable.containsINode(path)) {
            return -ErrorCodes.ENONET();
        }
        // delete the file if there are no more hard links
        return 0;
    }

    @Override
    public int mkdir(String path, long mode) {
        return 0;
    }

    @Override
    public int rmdir(String path) {
        return 0;
    }

    @Override
    public int rename(String oldpath, String newpath) {
        return 0;
    }

    @Override
    public int truncate(String path, @size_t long size) {
        return 0;
    }

    @Override
    public int release(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int setxattr(String path, String name, Pointer value, @size_t long size, int flags) {
        return 0;
    }

    @Override
    public int getxattr(String path, String name, Pointer value, @size_t long size) {
        return 0;
    }

    @Override
    public int listxattr(String path, Pointer list, @size_t long size) {
        return 0;
    }

    @Override
    public int removexattr(String path, String name) {
        return 0;
    }

    @Override
    public int opendir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int releasedir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public void destroy(Pointer initResult) {
        if (isVisualised()) {
            try {
                visualiser.stopConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int access(String path, int mask) {
        return 0;
    }

    @Override
    public int lock(String path, FuseFileInfo fi, int cmd, Flock flock) {
        return 0;
    }

    public static void main(String[] args) {
        MemoryFS fs = new MemoryFS();
        try {
            fs.mount(args, true);
        } finally {
            fs.unmount();
        }
    }
}
