/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.alert.AlertResponse;

public interface AlertService {

    AlertResponse generateAlerts(Long uploadHistoryId);

    AlertResponse getAlerts(Long uploadHistoryId);

    AlertResponse markAllAsRead(Long uploadHistoryId);

    AlertResponse markAsRead(Long uploadHistoryId, Long alertId);
}
