/*!
 * @copyright
 * Copyright (c) 2017-2019 Intel Corporation
 *
 * @copyright
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * @copyright
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * @copyright
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @file get_pwm.cpp
 *
 * @brief GetPwm IPMI command request and response implementation.
 * */

#include "ipmi/command/sdv/rmm/get_pwm.hpp"
#include "ipmi/command/sdv/enums.hpp"

using namespace ipmi;
using namespace ipmi::command::sdv;

request::GetPwm::GetPwm() : Request(NetFn::INTEL, Cmd::GET_PWM) {}

void request::GetPwm::pack(IpmiInterface::ByteBuffer& data) const {
    data.push_back(m_asset_index);
}

response::GetPwm::GetPwm(): Response(NetFn::INTEL + 1, Cmd::GET_PWM, RESPONSE_SIZE) {}

void response::GetPwm::unpack(const IpmiInterface::ByteBuffer& data) {
    m_pwm_value = data[OFFSET_DATA];
}

std::uint8_t response::GetPwm::get_pwm_value() const {
    return m_pwm_value;
}
