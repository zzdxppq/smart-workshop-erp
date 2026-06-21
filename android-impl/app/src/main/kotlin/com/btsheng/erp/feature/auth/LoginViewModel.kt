package com.btsheng.erp.feature.auth

import androidx.lifecycle.ViewModel
import com.btsheng.erp.core.network.AuthApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val authApi: AuthApi,
) : ViewModel()
