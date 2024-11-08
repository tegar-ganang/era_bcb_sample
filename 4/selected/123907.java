package jpfm.mount;

/**
 * Use {@link jpfm.MountFlags.Builder } for creating instances.
 * @author Shashank Tulsyan
 */
public final class MountFlags {

    private int mountFlags;

    MountFlags() {
    }

    MountFlags(int mountFlags) {
        this.mountFlags = mountFlags;
    }

    public final int getMountFlag() {
        return mountFlags;
    }

    /**
     * The desktop flag is only used on Mac and Linux (for now). Causes PFM to
     * auto-create the mount point under /Volumes (mac) or /media (linux). <br/>
     * Although desktop and folder flags do not cause conflict in formatter
     * (desktop flag overrides mountlocation setting), JPfm does
     * not allow this, so that implementor realizes that desktop flag
     * overrides mountlocation setting.
     */
    public final boolean isDesktop() {
        return (mountFlags & 0x0200) == 0x0200;
    }

    final boolean isFolder() {
        return (mountFlags & 0x0040) == 0x0040;
    }

    /**
     * Allows the operating system to buffer file data into
     * some cache for faster data access. <br/>
     * <u>Note :</u> By default fileSystem are mounted with
     * forceunbuffered flag.
     * @see MountFlags#isForceUnbuffered() 
     */
    public final boolean isForceBuffered() {
        return (mountFlags & 0x0100) == 0x0100;
    }

    /**
     * Prevents the operating system from buffering. <br/>
     * What happens without this is, the size of the request
     * reported would be larger than the actual.
     * This is undesired if the content is being downloaded,
     * because more than what is required may be downloaded.
     * If you are mounting on top of a existing Network FileSystem
     * (or NeembuuVirtualFileSystem), be sure to set forceunBuffered.
     */
    public final boolean isForceUnbuffered() {
        return (mountFlags & 0x0080) == 0x0080;
    }

    /**
     * The read only flag is passed through to the pismo formatter, but otherwise
     * is unused.
     * JPfm ignore this flag. This flag will work when the pismo formatter uses it.
     * You can use {@link JPfmBasicFileSystem }.
     * It is easy to make concurrent readonly filesystems as well.
     * For doing this, you can return {@link JPfmError#ACCESS_DENIED } for
     * write requests, and any other operation that it does not wish to support.
     * 
     * Implementor should also realize that this is different from
     * {@link FileFlags#isReadOnly() }.
     * @see JPfmBasicFileSystem
     * @see JPfmError#ACCESS_DENIED
     * @see FileFlags#isReadOnly() 
     */
    public final boolean isReadOnly() {
        return (mountFlags & 0x0001) == 0x0001;
    }

    /**
     * System visible determines whether other sessions (fast user
     * switching, system service session, terminal server) can see the virtual
     * folder. Drive letter and UNC access is unaffected. <br/>
     * This is different from {@link jpfm.VolumeVisibility#GLOBAL }
     * @see jpfm.VolumeVisibility#GLOBAL
     */
    public final boolean isSystemVisible() {
        return (mountFlags & 0x0002) == 0x0002;
    }

    /**
     * No virtual folder is created. The only way to access the virtual folder is by UNC path.
     * Example : <br/>
     * In windows C:\VirtualFolder is equivalent to UNC \\?\C:\VirtualFolder <br/>
     * So, if this flag is set, virtual folder C:\VirtualFolder would be inaccessible but
     * \\?\C:\VirtualFolder would be accessible.
     * Further reference : http://en.wikipedia.org/wiki/Path_%28computing%29#Uniform_Naming_Convention
     */
    public final boolean isUncOnly() {
        return (mountFlags & 0x0010) == 0x0010;
    }

    /**
     * Pismo formatter log messages are printed to standard output stream.
     * May be used for debugging.
     * JPfm log messages are not affected by this flag.
     */
    public final boolean isVerbose() {
        return (mountFlags & 0x0020) == 0x0020;
    }

    /**
     * It changes the access control lists that are
     * generated for files/folders in the volume. On unix it changes the
     * file mode that is generated for file/folders in the volume.
     */
    public final boolean isWorldRead() {
        return (mountFlags & 0x0004) == 0x0004;
    }

    /**
     * It is used by the pfm to determine if users other than the one creating the mount have
     * write access. <br/>
     *
     * World write and Readonlyflags do not conflict. Readonly flag is not
     */
    public final boolean isWorldWrite() {
        return (mountFlags & 0x0008) == 0x0008;
    }

    public static final class Builder {

        private int mountFlags;

        public Builder() {
        }

        public final int getMountFlag() {
            return mountFlags;
        }

        public final boolean isDesktop() {
            return (mountFlags & 0x0200) == 0x0200;
        }

        public final boolean isForceBuffered() {
            return (mountFlags & 0x0100) == 0x0100;
        }

        public final boolean isForceUnbuffered() {
            return (mountFlags & 0x0080) == 0x0080;
        }

        public final boolean isReadOnly() {
            return (mountFlags & 0x0001) == 0x0001;
        }

        public final boolean isSystemVisible() {
            return (mountFlags & 0x0002) == 0x0002;
        }

        public final boolean isUncOnly() {
            return (mountFlags & 0x0010) == 0x0010;
        }

        public final boolean isVerbose() {
            return (mountFlags & 0x0020) == 0x0020;
        }

        public final boolean isWorldRead() {
            return (mountFlags & 0x0004) == 0x0004;
        }

        public final boolean isWorldWrite() {
            return (mountFlags & 0x0008) == 0x0008;
        }

        public final Builder setDesktop() {
            mountFlags |= 0x0200;
            return this;
        }

        public final Builder setForceBuffered() {
            if (isForceUnbuffered()) throw new IllegalStateException("Mount flag already set to unbuffered");
            mountFlags |= 0x0100;
            return this;
        }

        public final Builder setForceUnbuffered() {
            if (isForceBuffered()) throw new IllegalStateException("Mount flag already set to buffered");
            mountFlags |= 0x0080;
            return this;
        }

        public final Builder setReadOnly() {
            if (isWorldWrite()) throw new IllegalStateException("Mount flag already set to worldwrite. Flag cannot indicate readonly and world write at the same time.");
            mountFlags |= 0x0001;
            return this;
        }

        public final Builder setSystemVisible() {
            mountFlags |= 0x0002;
            return this;
        }

        /**
         * Unsupported on windows for a reason which i forgot
         * @throws UnsupportedOperationException
         */
        public final Builder setUncOnly() {
            mountFlags |= 0x0010;
            return this;
        }

        public final Builder setVerbose() {
            mountFlags |= 0x0020;
            return this;
        }

        /**
         * This does not serve any purpose. Volumes are by default, are atleast world read.
         */
        public final Builder setWorldRead() {
            mountFlags |= 0x0004;
            return this;
        }

        public final Builder setWorldWrite() {
            if (isReadOnly()) throw new IllegalStateException("Mount flag already set to readonly. Flag cannot indicate readonly and world write at the same time.");
            mountFlags |= 0x0008;
            return this;
        }

        public final MountFlags build() {
            return new MountFlags(mountFlags);
        }
    }
}
