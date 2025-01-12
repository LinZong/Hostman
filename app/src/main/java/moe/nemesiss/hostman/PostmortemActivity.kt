package moe.nemesiss.hostman

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import moe.nemesiss.hostman.databinding.ActivityPostmortemBinding
import moe.nemesiss.hostman.fragment.PostmortemExpertActionPanelFragment
import moe.nemesiss.hostman.fragment.PostmortemSimpleActionPanelFragment
import moe.nemesiss.hostman.model.viewmodel.PostmortemViewModel

class PostmortemActivity : AppCompatActivity(), FragmentOnAttachListener {

    private val binding by lazy { ActivityPostmortemBinding.inflate(layoutInflater) }

    private val viewModel by viewModels<PostmortemViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.stackTrace.value = intent.getStringExtra(STACKTRACE_KEY)
        viewModel.actionPanelMode.observe(this) {
            if (it != null) {
                when (it) {
                    PostmortemViewModel.ActionPanelMode.SIMPLE -> showSimpleActionPanel()
                    PostmortemViewModel.ActionPanelMode.EXPERT -> showExpertActionPanel()
                }
            }
        }
        supportFragmentManager.addFragmentOnAttachListener(this)
    }

    private fun showSimpleActionPanel() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val tx = supportFragmentManager.beginTransaction()
            removeAllFragments(tx)
            val fragment = PostmortemSimpleActionPanelFragment()
            tx.add(R.id.postmortem_action_panel_container, fragment)
            tx.show(fragment)
            tx.commit()
        }
    }

    private fun showExpertActionPanel() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val tx = supportFragmentManager.beginTransaction()
            removeAllFragments(tx)
            val fragment = PostmortemExpertActionPanelFragment()
            tx.add(R.id.postmortem_action_panel_container, fragment)
            tx.show(fragment)
            tx.commit()
        }
    }

    private fun removeAllFragments(tx: FragmentTransaction) {
        supportFragmentManager.fragments.forEach { tx.hide(it) }
    }

    companion object {

        private const val STACKTRACE_KEY = "stacktrace"

        @JvmStatic
        fun buildIntent(context: Context, t: Throwable): Intent {
            val intent = Intent(context, PostmortemActivity::class.java)
            intent.putExtra(STACKTRACE_KEY, t.stackTraceToString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
    }


    override fun onAttachFragment(p0: FragmentManager, fragment: Fragment) {
        if (fragment is PostmortemSimpleActionPanelFragment) {
            fragment.prepare(viewModel)
        }
        if (fragment is PostmortemExpertActionPanelFragment) {
            fragment.prepare(viewModel)
        }
    }
}