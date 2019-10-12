#import <Cordova/CDV.h>

@interface FirebasePlugin : CDVPlugin

- (void)logEvent:(CDVInvokedUrlCommand*)command;
- (void)logError:(CDVInvokedUrlCommand*)command;
- (void)setCrashlyticsUserId:(CDVInvokedUrlCommand*)command;
- (void)setScreenName:(CDVInvokedUrlCommand*)command;
- (void)setUserId:(CDVInvokedUrlCommand*)command;
- (void)setAnalyticsCollectionEnabled:(CDVInvokedUrlCommand*)command;
- (void)setPerformanceCollectionEnabled:(CDVInvokedUrlCommand*)command;
- (void)setCrashlyticsCollectionEnabled:(CDVInvokedUrlCommand*)command;
- (void)logMessage:(CDVInvokedUrlCommand*)command;
- (void)sendCrash:(CDVInvokedUrlCommand*)command;

- (void) handlePluginExceptionWithContext: (NSException*) exception :(CDVInvokedUrlCommand*)command;
- (void) handlePluginExceptionWithoutContext: (NSException*) exception;
- (void)_logError: (NSString*)msg;

@end
