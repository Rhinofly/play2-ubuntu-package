package com.lunatech.play.ubuntupackage

import com.typesafe.packager
import com.typesafe.packager._
import com.typesafe.packager.debian.DebianPlugin
import com.typesafe.packager.Keys._
import sbt._
import sbt.Keys._

object UbuntuPackagePlugin extends Plugin with DebianPlugin {
  object UbuntuPackageKeys {
    def user = SettingKey[String]("ubuntu-user", "Ubuntu system user for this application")
    def group = SettingKey[String]("ubuntu-group", "Ubuntu system group for this application")
    def installationDirectory = TaskKey[String]("ubuntu-installation-directory", "Installation dir for this application")
    def port = SettingKey[Int]("ubuntu-default-port", "Default Port")
    def configUrl = SettingKey[String]("ubuntu-config-url", "Url that is passed to the config.url propery of the application")
    def applicationConfiguration = TaskKey[ApplicationConfiguration]("ubuntu-application-configuration", "Application configuration")
    def maintainer = packager.Keys.maintainer 
    
    def deb = TaskKey[File]("deb", "Build the 'deb' package")
    // TODO: It's nicer to have these tasks just generate tuples of name/content/perms/user/group, and then make a sequence of them in a single task.
    def upstartConfig = TaskKey[File]("ubuntu-upstart-config", "Create the Ubuntu upstart config file")
    def preInstall = TaskKey[Option[File]]("ubuntu-pre-install", "Create the Ubuntu preinst file")
    def postInstall = TaskKey[Option[File]]("ubuntu-post-install", "Create the Ubuntu postinst file")
    def preRemoval = TaskKey[Option[File]]("ubuntu-pre-removal", "Create the Ubuntu prerm file")
    def postRemoval = TaskKey[Option[File]]("ubuntu-post-removal", "Create the Ubuntu postrm file")
  }
  
  import UbuntuPackageKeys._
  
  lazy val ubuntuPackageSettings: Seq[Project.Setting[_]] = linuxSettings ++ debianSettings ++ Seq(
    name in Debian <<= normalizedName,
    version in Debian <<= version,
    user <<= normalizedName,
    group <<= user,
    port := 9000,
    maintainer := "Unknown maintainer",
    packageDescription <<= description,
    packageSummary <<= description,
    installationDirectory <<= (name in Debian) map ("/opt/" + _),
    
    debianPackageDependencies in Debian ++= Seq("java2-runtime", "upstart (>= 1.5)"),
    
    applicationConfiguration <<= (name in Debian, user, group, installationDirectory, port, configUrl) map {
      (name, user, group, dir, port, configUrl) => ApplicationConfiguration(name, user, group, dir, port, configUrl)
    },
    
    linuxPackageMappings <++= 
      (baseDirectory, target, applicationConfiguration, packageSummary, play.Project.dist, upstartConfig) map {
      (baseDir, targetDir, appConfig, descriptionValue, distZip, upstartConfig) =>
        val applicationDir = "opt/%s" format appConfig.name
        val distDir = targetDir / "dist-zip"
        IO.delete(distDir)
        IO.unzip(distZip, distDir)

        val unpackedAppDir: File = (distDir * new FileFilter { def accept(f: File) = f.isDirectory }).get.head
        
        Seq(
          packageMapping(unpackedAppDir -> applicationDir) withUser(appConfig.user) withGroup(appConfig.group) withPerms("0777"),
          packageMapping(upstartConfig -> "/etc/init/%s.conf".format(appConfig.name)) withPerms("0644") withConfig()
        ) ++ (for {
          path <- (unpackedAppDir ***).get
          if !path.isDirectory
        } yield {
          val mapping = packageMapping(path -> path.toString.replaceFirst(unpackedAppDir.toString, applicationDir)) withUser(appConfig.user) withGroup(appConfig.group) withPerms("0644")
          if(path.toString.endsWith("start")) {
            mapping withPerms("0755")
          } else {
            mapping
          }
        })
        
    },

    deb <<= packageBin in Debian,
    
    upstartConfig <<= (target, applicationConfiguration) map { (dir, config) =>
      val file = dir / "%s.upstart".format(config.name)
      IO.write(file, FilesGenerator.upstartScript(config))
      file
    },
    preInstall <<= (target in Debian, applicationConfiguration) map { (dir, config) => 
      writeDebianScript(dir / "DEBIAN" / "preinst", FilesGenerator.preInstall(config))
    },
    postInstall <<= (target in Debian, applicationConfiguration) map { (dir, config) => 
      writeDebianScript(dir / "DEBIAN" / "postinst", FilesGenerator.postInstall(config))
    },
    preRemoval <<= (target in Debian, applicationConfiguration) map { (dir, config) => 
      writeDebianScript(dir / "DEBIAN" / "prerm", FilesGenerator.preRemoval(config))
    },
    postRemoval <<= (target in Debian, applicationConfiguration) map { (dir, config) => 
      writeDebianScript(dir / "DEBIAN" / "postrm", FilesGenerator.postRemoval(config))
    },
    (debianExplodedPackage in Debian) <<= (debianExplodedPackage in Debian) dependsOn (preInstall, postInstall, preRemoval, postRemoval)
    
  ) ++
  SettingsHelper.makeDeploymentSettings(Debian, packageBin in Debian, "deb")
  
  def writeDebianScript(file: File, content: Option[String]) = {
    content.map { content => 
      IO.write(file, content)
      Seq("chmod", "+x", file.getAbsolutePath).!
      file
    }
  }
}