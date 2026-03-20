// IShizukuGrantService.aidl
package dev.perfoverlay.service;

interface IShizukuGrantService {
    /**
     * Grant SYSTEM_ALERT_WINDOW permission via appops.
     * Returns true if the grant succeeded.
     */
    boolean grantOverlayPermission();
}
