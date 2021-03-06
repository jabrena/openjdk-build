println "building ${JDK_VERSION}"

def buildPlatforms = ['Mac', 'Linux', 'zLinux', 'ppc64le', 'Windows', 'AIX', 'arm64']
def buildMaps = [:]
buildMaps['Mac'] = [test:true, ArchOSs:'x86-64_macos']
buildMaps['Windows'] = [test:false, ArchOSs:'x86-64_windows']
buildMaps['Linux'] = [test:true, ArchOSs:'x86-64_linux']
buildMaps['zLinux'] = [test:true, ArchOSs:'s390x_linux']
buildMaps['ppc64le'] = [test:true, ArchOSs:'ppc64le_linux']
buildMaps['AIX'] = [test:false, ArchOSs:'ppc64_aix']
buildMaps['arm64'] = [test:true, ArchOSs:'arm64_linux']
def typeTests = ['openjdktest', 'systemtest']

def jobs = [:]
for ( int i = 0; i < buildPlatforms.size(); i++ ) {
	def index = i
	def platform = buildPlatforms[index]
	def archOS = buildMaps[platform].ArchOSs
	jobs[platform] = {
		def buildJob
		def buildJobNum
		def checksumJob
		stage('build') {
			buildJob = build job: "openjdk9_build_${archOS}"
			buildJobNum = buildJob.getNumber()
		}
		if (buildMaps[platform].test) {
			stage('test') {
				typeTests.each {
					build job:"openjdk9_hs_${it}_${archOS}",
							propagate: false,
							parameters: [string(name: 'UPSTREAM_JOB_NUMBER', value: "${buildJobNum}"),
									string(name: 'UPSTREAM_JOB_NAME', value: "openjdk9_build_${archOS}")]
				}
			}
		}
		stage('checksums') {
			checksumJob = build job: 'openjdk9_build_checksum',
							parameters: [string(name: 'UPSTREAM_JOB_NUMBER', value: "${buildJobNum}"),
									string(name: 'UPSTREAM_JOB_NAME', value: "openjdk9_build_${archOS}")]
		}
		stage('publish nightly') {
			build job: 'openjdk_release_tool',
						parameters: [string(name: 'REPO', value: 'nightly'),
									string(name: 'TAG', value: 'jdk-9+181'),
									string(name: 'VERSION', value: 'jdk9'),
									string(name: 'CHECKSUM_JOB_NAME', value: "openjdk9_build_checksum"),
									string(name: 'CHECKSUM_JOB_NUMBER', value: "${checksumJob.getNumber()}")]
		}
	}
}
parallel jobs
