/*!
 * @brief RmmTelemetryService declaration
 *
 * @copyright Copyright (c) 2017-2019 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @file rmm_telemetry_service.hpp
 */

#pragma once



#include "telemetry/rmm_telemetry_service_interface.hpp"



namespace agent {
namespace rmm {

/*!
 * @brief Entry point for telemetry functionality.
 */
class RmmTelemetryService final : public RmmTelemetryServiceInterface {
    using ResourceSensorPtr = discovery::helpers::ResourceSensorPtr;
public:

    virtual ~RmmTelemetryService();


    /*!
     * @brief Constructor
     */
    RmmTelemetryService();


    virtual const std::vector<ResourceSensorPtr>& get_resource_sensors() const override {
        return m_resource_sensors;
    };


private:
    std::vector<ResourceSensorPtr> m_resource_sensors;
};

}
}
