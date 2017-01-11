
package fr.cnrs.ibmp.dataSets;

/**
 * Utility class for working with {@link DataSource}s.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public final class DataSources {

	private DataSources() {
		// NB: prevent instantiation of utility class.
	}

	public static DataSource newDataSource(final DataSource dataSource) {
		if (dataSource instanceof FileDataSource) {
			return new FileDataSource((FileDataSource) dataSource);
		}
		else if (dataSource instanceof ImagePlusDataSource) {
			return new ImagePlusDataSource((ImagePlusDataSource) dataSource);
		}

		return null;
	}
}
