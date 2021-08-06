#import "PusherPlugin.h"
#if __has_include(<pusher/pusher-Swift.h>)
#import <pusher/pusher-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "pusher-Swift.h"
#endif

@implementation PusherPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPusherPlugin registerWithRegistrar:registrar];
}
@end
