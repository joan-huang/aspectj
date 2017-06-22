package com.yy.plugin.aspectJ

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * AspectJ用于代码织入（比如调用方法前，调用方法后，javassist不支持）
 * Created by huangfan on 2017/6/22.
 */
public class AspectjPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.dependencies {
            compile 'org.aspectj:aspectjrt:1.8.9'
        }
        final def log = project.logger
        log.error "========================";
        log.error "Aspectj切片开始编织Class!";
        log.error "========================";


        def isAppProject = project.plugins.withType(AppPlugin)
        def isLibProject = project.plugins.withType(LibraryPlugin)
        final def variants

        if (isAppProject) {
            variants = project.android.applicationVariants
        } else if (isLibProject) {
            variants = project.android.libraryVariants
        } else {
            throw new IllegalStateException("Must be android project or android-library project.")
        }

        project.afterEvaluate {
            variants.all{variant->
                def buildTypeName = variant.name.capitalize()

                def aopTask = project.task("compile${buildTypeName}AspectJ") {
                    doLast {
                        String[] args = ["-showWeaveInfo",
                                         "-1.8",
                                         "-inpath", javaCompile.destinationDir.toString(),
                                         "-aspectpath", javaCompile.classpath.asPath,
                                         "-d", javaCompile.destinationDir.toString(),
                                         "-classpath", javaCompile.classpath.asPath,
                                         "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]

                        MessageHandler handler = new MessageHandler(true);
                        new Main().run(args, handler);
                        for (IMessage message : handler.getMessages(null, true)) {
                            switch (message.getKind()) {
                                case IMessage.ABORT:
                                case IMessage.ERROR:
                                case IMessage.FAIL:
                                    log.error message.message, message.thrown
                                    break;
                                case IMessage.WARNING:
                                    log.warn message.message, message.thrown
                                    break;
                                case IMessage.INFO:
                                    log.info message.message, message.thrown
                                    break;
                                case IMessage.DEBUG:
                                    log.debug message.message, message.thrown
                                    break;
                            }
                        }
                    }
                }
                def hasLambdaPlugin=project.plugins.hasPlugin('me.tatarka.retrolambda')
                if(hasLambdaPlugin) {
                    project.tasks["compileRetrolambda$buildTypeName"].finalizedBy(aopTask)
                }else{
                    variant.javaCompile.finalizedBy(aopTask)
                }
            }
        }
    }
}