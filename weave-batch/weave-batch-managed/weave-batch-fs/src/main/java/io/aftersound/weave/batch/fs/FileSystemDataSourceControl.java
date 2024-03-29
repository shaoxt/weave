package io.aftersound.weave.batch.fs;

import io.aftersound.weave.batch.jobspec.datasource.DataSourceControl;
import io.aftersound.weave.common.NamedType;

public class FileSystemDataSourceControl extends DataSourceControl {

    public static final NamedType<DataSourceControl> TYPE = NamedType.of("FS", FileSystemDataSourceControl.class);

    @Override
    public String getType() {
        return TYPE.name();
    }

}
