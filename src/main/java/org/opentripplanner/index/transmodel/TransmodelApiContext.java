package org.opentripplanner.index.transmodel;

import org.opentripplanner.standalone.Router;

public class TransmodelApiContext {
        public final Router router;
        public final String clientName;

        TransmodelApiContext(Router router, String clientName) {
                this.router = router;
                this.clientName = clientName;
        }
}
