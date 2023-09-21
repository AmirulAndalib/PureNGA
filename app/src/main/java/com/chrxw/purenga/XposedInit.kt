package com.chrxw.purenga

import android.app.AndroidAppHelper
import android.app.Application
import android.app.Instrumentation
import android.widget.Toast
import com.chrxw.purenga.utils.Helper
import com.chrxw.purenga.utils.Helper.log
import com.github.kyuubiran.ezxhelper.AndroidLogger
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage


/**
 * 初始化Xposed
 */
class XposedInit : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 初始化EzHelper
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(Constant.LOG_TAG)

        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            AndroidLogger.d("模块内运行")

            MethodFinder.fromClass(MainActivity.Companion::class.java.name).filterByName("isModuleActive").first()
                .createHook {
                    replace {
                        it.log()
                        return@replace true
                    }
                }

        } else if (lpparam.packageName == Constant.NGA_PACKAGE_NAME) {
            AndroidLogger.d("NGA内运行")


//            XposedHelpers.findAndHookMethod("android.widget.Toast", lpparam.classLoader, "makeText",
//                Context::class.java,
//                CharSequence::class.java,
//                Int::class.javaPrimitiveType, object : XC_MethodHook() {
//                    @Throws(Throwable::class)
//                    override fun beforeHookedMethod(param: MethodHookParam) {
//                        try {
//                            val str = param.args[1] as String
//                            AndroidLogger.i("toast $str")
//
//                            throw Exception("test")
//                        } catch (e: Exception) {
//                            AndroidLogger.e("toastMessage", e)
//                        }
//                    }
//                })

            MethodFinder.fromClass(Instrumentation::class.java).filterByName("callApplicationOnCreate")
                .filterByAssignableParamTypes(Application::class.java).first().createHook {
                    after {
                        it.log()

                        if (it.args[0] is Application) {
                            if (!isInit) {
                                isInit = true
                                val context = AndroidAppHelper.currentApplication().applicationContext

                                EzXHelper.initAppContext(context, true)

                                val error = Hooks.initHooks(lpparam.classLoader)
                                if (error == 0) {
                                    if (!Helper.getSpBool(Constant.HIDE_HOOK_INFO, false)) {
                                        Helper.toast(
                                            "PureNGA 加载成功, 请到【设置】>【PureNGA】开启功能",
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                } else {
                                    val ngaVersion = Helper.getNgaVersion()
                                    Helper.toast(
                                        "PureNGA $error 个模块加载失败, 可能不支持当前版本\nNGA 版本: $ngaVersion\n插件版本: ${BuildConfig.VERSION_NAME}",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            } else {
                                AndroidLogger.d("跳过初始化")
                            }
                        }
                    }
                }
        }
    }

    companion object {
        private var isInit = false
    }
}


