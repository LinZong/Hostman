// IFileProvider.aidl
package moe.nemesiss.hostman;

// Declare any non-default types here with import statements

interface IFileProvider {
	String getFileTextContent(String filePath) = 1;

	byte[] getFileBytes(String filePath) = 2;

	String writeFileBytes(String filePath, in byte[] fileContent) = 3;

	int getPid() = 4;

	int getUid() = 5;

	void destroy()  = 16777114; // Destroy method defined by Shizuku server
}